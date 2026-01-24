# Oracle SELECT FOR UPDATE NOWAIT & MyBatis 재시도 전략

본 문서는 **H2 Database(Oracle Mode)** 및 **Oracle** 환경에서 `SELECT FOR UPDATE NOWAIT` 사용 시 발생하는 락 충돌을 **전략 패턴(Strategy Pattern)**으로 해결하는 재시도 로직 구현 가이드입니다.

---

## 🛠 기술 스택
*   **Backend**: JDK 21, Spring Boot, MyBatis, Lombok
*   **Database**: H2 Database (Oracle Mode)
*   **Design Pattern**: Strategy Pattern (for retry logic)

---

## 1. Database & Persistence Layer

### 계좌 테이블 생성 (DDL)
동시성 제어가 필요한 예금 계좌 테이블 설계입니다.

```sql
-- src/main/resources/schema.sql
CREATE TABLE ACCOUNT (
    ACCOUNT_NO   VARCHAR2(20) PRIMARY KEY, -- 계좌번호
    USER_NAME    VARCHAR2(50) NOT NULL,    -- 소유주
    BALANCE      NUMBER(18, 2) DEFAULT 0,  -- 잔액
    UPDATE_DATE  TIMESTAMP DEFAULT SYSDATE -- 최종 수정일
);
```

### 테스트 데이터 삽입
```sql
-- src/main/resources/data.sql
INSERT INTO ACCOUNT (ACCOUNT_NO, USER_NAME, BALANCE) VALUES ('123-456', 'Gemini', 1000000);
```

### MyBatis Mapper (AccountMapper.java & AccountMapper.xml)
`SELECT FOR UPDATE NOWAIT`를 사용하여 계좌 잔액 조회 시 즉시 락을 시도하고, 락 획득 실패 시 예외를 발생시킵니다.

```java
// src/main/java/com/example/lockretry/mapper/AccountMapper.java
@Mapper
public interface AccountMapper {
    AccountDto selectAccountForUpdate(@Param("accountNo") String accountNo);
    int updateBalance(AccountDto account);
}
```

```xml
<!-- src/main/resources/mappers/AccountMapper.xml -->
<select id="selectAccountForUpdate" resultType="com.example.lockretry.domain.AccountDto">
    SELECT
        ACCOUNT_NO,
        USER_NAME,
        BALANCE,
        UPDATE_DATE
    FROM ACCOUNT
    WHERE ACCOUNT_NO = #{accountNo}
    FOR UPDATE NOWAIT
</select>

<update id="updateBalance">
    UPDATE ACCOUNT
    SET
        BALANCE = #{balance},
        UPDATE_DATE = SYSDATE
    WHERE
        ACCOUNT_NO = #{accountNo}
</update>
```

---

## 2. 재시도 로직 구현

### RetryStrategy 인터페이스
재시도 정책을 추상화합니다.

```java
// src/main/java/com/example/lockretry/strategy/RetryStrategy.java
public interface RetryStrategy {
    boolean shouldRetry(Exception e, int attemptCount);
    long getWaitTime(int attemptCount);
}
```

### RandomBackoffRetryStrategy 구현체
락 충돌 시 `PessimisticLockingFailureException` (Oracle의 ORA-00054와 유사) 발생 시 랜덤 백오프 방식으로 재시도합니다.

```java
// src/main/java/com/example/lockretry/strategy/RandomBackoffRetryStrategy.java
@Component
public class RandomBackoffRetryStrategy implements RetryStrategy {
    private static final int MAX_RETRIES = 3;
    private static final long BASE_WAIT_TIME_MS = 100; // 기본 대기 0.1초
    private static final long MAX_JITTER_MS = 200;     // 랜덤 추가 대기 최대 0.2초

    @Override
    public boolean shouldRetry(Exception e, int attemptCount) {
        if (e instanceof PessimisticLockingFailureException) {
            return attemptCount < MAX_RETRIES;
        }
        return false;
    }

    @Override
    public long getWaitTime(int attemptCount) {
        return BASE_WAIT_TIME_MS + ThreadLocalRandom.current().nextLong(MAX_JITTER_MS);
    }
}
```

### LockRetryTemplate
실제 비즈니스 로직(Supplier)을 감싸 재시도 로직을 적용하는 템플릿입니다. `RetryStrategy`를 주입받아 유연성을 확보합니다.

```java
// src/main/java/com/example/lockretry/component/LockRetryTemplate.java
@Slf4j
@Component
@RequiredArgsConstructor
public class LockRetryTemplate {
    private final RetryStrategy retryStrategy;

    public <T> T execute(Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (Exception e) {
                if (retryStrategy.shouldRetry(e, attempt)) {
                    attempt++;
                    long waitTime = retryStrategy.getWaitTime(attempt);
                    log.warn("Lock conflict detected (ORA-00054). Retrying attempt {}/{} after {}ms...",
                            attempt, 3, waitTime); // MAX_RETRIES는 RandomBackoffRetryStrategy에서 관리되므로 상수 대신 메시지에 3을 명시
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    throw e; // 재시도 대상이 아니거나 횟수 초과 시 예외 전파
                }
            }
        }
    }
}
```

---

## 3. 서비스 계층 통합

### AccountService
`@Transactional` 하에서 `LockRetryTemplate`을 사용하여 `SELECT FOR UPDATE` 호출을 래핑하고, 입금 비즈니스 로직을 수행합니다.

```java
// src/main/java/com/example/lockretry/service/AccountService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountMapper accountMapper;
    private final LockRetryTemplate lockRetryTemplate;

    @Transactional
    public void deposit(String accountNo, BigDecimal amount) {
        log.debug("Deposit request - Account: {}, Amount: {}", accountNo, amount);

        // 1. 락 획득 (재시도 전략 적용)
        // 람다식을 통해 비즈니스 로직과 재시도 로직을 분리
        AccountDto account = lockRetryTemplate.execute(() ->
            accountMapper.selectAccountForUpdate(accountNo)
        );

        if (account == null) {
            log.warn("Account not found: {}", accountNo);
            throw new IllegalArgumentException("Account not found: " + accountNo);
        }

        log.debug("Lock acquired. Current Balance: {}", account.getBalance());

        // 2. 비즈니스 로직 수행
        account.setBalance(account.getBalance().add(amount));

        // 3. 업데이트
        accountMapper.updateBalance(account);
        log.debug("Deposit completed. New Balance: {}", account.getBalance());
    }
}
```

---

## 4. REST API 엔드포인트

### AccountController
입금 요청을 처리하는 REST 컨트롤러입니다.

```java
// src/main/java/com/example/lockretry/controller/AccountController.java
@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/api/deposit")
    public ResponseEntity<String> deposit(@RequestParam String accountNo, @RequestParam BigDecimal amount) {
        try {
            accountService.deposit(accountNo, amount);
            return ResponseEntity.ok("입금 성공: " + amount + "원 (계좌: " + accountNo + ")");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("입금 실패: " + e.getMessage());
        }
    }
}
```

---

## 5. 핵심 장점
*   **안정성**: `NOWAIT`을 사용하여 DB 세션이 무한 대기(Hang) 상태에 빠지는 것을 방지합니다. 락 획득 실패 시 즉시 예외를 발생시킵니다.
*   **유연성**: 재시도 횟수, 대기 정책(`RandomBackoffRetryStrategy`) 및 재시도 대상 예외를 `RetryStrategy` 구현체에서 한 곳에서만 수정하면 전역에 반영됩니다.
*   **가독성**: 비즈니스 로직에서 `try-catch` 및 재시도 루프가 `LockRetryTemplate`으로 추상화되어 코드가 깔끔해집니다.
*   **성능**: 랜덤 백오프(Random Backoff)를 통해 동시 요청 시 발생하는 재충돌(Collision)을 방지하고, 시스템 부하를 줄입니다.

---

## 6. Spring Boot 애플리케이션 시작

```java
// src/main/java/com/example/lockretry/LockRetryApplication.java
@SpringBootApplication
public class LockRetryApplication {
    public static void main(String[] args) {
        SpringApplication.run(LockRetryApplication.class, args);
    }
}
```

---

## 7. H2 Database 설정
`application.yml`에서 H2를 Oracle 호환 모드로 설정하여 `SELECT FOR UPDATE NOWAIT` 구문이 정상적으로 동작하도록 합니다.

```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=Oracle
    driverClassName: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console

mybatis:
  mapper-locations: classpath:/mappers/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```
---sql
-- 1. Auto Commit 해제 (트랜잭션 수동 제어)
SET AUTOCOMMIT FALSE;

SELECT ACCOUNT_NO, USER_NAME, BALANCE, UPDATE_DATE FROM ACCOUNT WHERE ACCOUNT_NO = '123-456'  FOR UPDATE ;
---