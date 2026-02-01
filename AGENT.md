# 락 재시도 메커니즘 (Lock Retry Mechanism)

고동시성 시나리오에서 데이터베이스 락 충돌을 처리하기 위한 분산 락 및 재시도 전략을 구현한 Spring Boot 애플리케이션입니다.

## 🎯 개요

이 프로젝트는 **전략 패턴(Strategy Pattern)**을 사용하여 `SELECT FOR UPDATE NOWAIT` 데이터베이스 락 충돌을 처리하는 강력한 재시도 메커니즘을 구현합니다. 재정 트랜잭션 시스템에서 안정적인 작동을 보장하기 위해 플러그인 가능한 재시도 정책, 포괄적인 모니터링, 스레드 안전 메트릭 수집을 제공합니다.

## ✨ 주요 기능

- **비관적 락킹**: 무기한 대기 대신 `SELECT FOR UPDATE NOWAIT`를 사용한 즉시 락 실패
- **전략 패턴**: 유연한 재시도 정책을 위한 플러그인 가능한 재시도 전략
- **랜덤 백오프**: 랜덤화된 대기 시간으로 재시도 충돌 방지
- **포괄적 모니터링**: 작업, 리소스, 성능에 대한 실시간 메트릭
- **스레드 안전 통계**: 안전한 메트릭 수집을 위한 동시 컬렉션
- **트랜잭션 관리**: 재시도를 위한 적절한 트랜잭션 경계 처리

## 🛠 기술 스택

- **런타임**: Java 21
- **프레임워크**: Spring Boot 3.2.4
- **영속성**: MyBatis 3.0.3
- **데이터베이스**: H2 Database (Oracle 모드)
- **유틸리티**: Lombok
- **테스트**: JUnit 5, Mockito

## 🏗 아키텍처

```
┌─────────────────────────────────────────┐
│         REST API 계층                  │
│  (AccountController, QueryController) │
├─────────────────────────────────────────┤
│         서비스 계층                    │
│      (AccountService)                  │
├─────────────────────────────────────────┤
│   재시도 템플릿 (공통 관심사)           │
│    (LockRetryTemplate)                │
├─────────────────────────────────────────┤
│       전략 계층                        │
│ (RetryStrategy, RandomBackoffStrategy) │
├─────────────────────────────────────────┤
│       모니터링 계층                     │
│ (LockMetricsCollector, Statistics)    │
├─────────────────────────────────────────┤
│     영속성 계층                        │
│  (MyBatis Mappers + XML)              │
├─────────────────────────────────────────┤
│      데이터베이스 계층                  │
│     (H2 - Oracle 모드)                │
└─────────────────────────────────────────┘
```

## 📁 프로젝트 구조

```
src/main/java/com/example/lockretry/
├── LockRetryApplication.java          # 메인 애플리케이션 진입점
├── component/
│   └── LockRetryTemplate.java         # 재시도 템플릿 래퍼
├── config/
│   └── TransactionConfig.java         # 트랜잭션 설정
├── controller/
│   ├── AccountController.java         # 변경 작업용 REST 엔드포인트
│   └── AccountQueryController.java     # 조회 작업용 REST 엔드포인트
├── domain/
│   └── AccountDto.java                # 데이터 전송 객체
├── mapper/
│   ├── AccountMapper.java             # 락킹 포함 읽기/쓰기 작업
│   └── AccountQueryMapper.java        # 읽기 전용 작업
├── monitoring/
│   ├── LockMetricsCollector.java      # 중앙 메트릭 수집
│   ├── LockOperationMetrics.java      # 작업별 메트릭
│   ├── OperationStats.java            # 작업 수준 통계
│   └── ResourceStats.java             # 리소스 수준 통계
├── service/
│   └── AccountService.java            # 비즈니스 로직 계층
└── strategy/
    ├── RetryStrategy.java             # 재시도 전략 인터페이스
    └── RandomBackoffRetryStrategy.java# 랜덤 백오프 구현

src/main/resources/
├── application.yml                    # 애플리케이션 설정
├── schema.sql                         # 데이터베이스 스키마
├── data.sql                           # 테스트 데이터
└── mappers/
    ├── AccountMapper.xml              # SQL 매핑
    └── AccountQueryMapper.xml         # 쿼리 매핑
```

## 🚀 시작하기

### 사전 요구사항

- Java 21 이상
- Gradle 7.0 이상

### 설치 및 실행

1. **저장소 복제**
   ```bash
   git clone <repository-url>
   cd lockReTry
   ```

2. **애플리케이션 빌드**
   ```bash
   ./gradlew build
   ```

3. **애플리케이션 실행**
   ```bash
   ./gradlew bootRun
   ```

4. **H2 콘솔 접속** (선택사항)
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:testdb`
   - 사용자명: `sa`
   - 비밀번호: (비워둠)

## 📡 API 엔드포인트

### 입금 (락킹 포함)
```http
POST /api/deposit
Content-Type: application/x-www-form-urlencoded

accountNo=123-456&amount=1000.50
```

### 계좌 조회 (락킹 없음)
```http
GET /api/account/123-456
```

**응답:**
```json
{
  "accountNo": "123-456",
  "userName": "Gemini",
  "balance": 1001000.50,
  "updateDate": "2024-01-25T10:30:00"
}
```

## ⚙️ 설정

### 데이터베이스 설정
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=Oracle;LOCK_TIMEOUT=0
    driverClassName: org.h2.Driver
    username: sa
    password:
```

### 재시도 전략 파라미터
- **최대 재시도 횟수**: 10
- **기본 대기 시간**: 100ms
- **최대 지터**: 200ms
- **최대 대기 시간**: 2000ms (2초)

### 커넥션 풀 (HikariCP)
- 최대 풀 크기: 10
- 최소 유휴: 5
- 커넥션 타임아웃: 30초
- 유휴 타임아웃: 10분
- 최대 수명: 30분

## 🧩 핵심 컴포넌트

### LockRetryTemplate
전략 패턴을 사용하여 비즈니스 작업에 재시도 로직을 래핑합니다:

```java
@Component
public class LockRetryTemplate {
    public <T> T execute(Supplier<T> action) {
        while (true) {
            try {
                return action.get();
            } catch (Exception e) {
                if (retryStrategy.shouldRetry(e, attempt)) {
                    // 대기 후 재시도
                } else {
                    throw e;
                }
            }
        }
    }
}
```

### RetryStrategy 인터페이스
```java
public interface RetryStrategy {
    boolean shouldRetry(Exception e, int attemptCount);
    long getWaitTime(int attemptCount);
}
```

### RandomBackoffRetryStrategy
재시도 충돌을 방지하기 위한 랜덤 백오프 구현:
- 대기 시간: `기본대기시간 + (시도횟수 * 50ms) + 랜덤(0, 최대지터)`
- 감지: `PessimisticLockingFailureException`, `ORA-00054`, 락 충돌

## 📊 모니터링 및 메트릭

### 전체 통계
- 총 실행 작업 수
- 성공률 (%)
- 작업당 평균 재시도 횟수
- 작업당 평균 대기 시간

### 작업별 통계
- 작업 유형 (DEPOSIT, WITHDRAW 등)
- 실행 횟수 (전체, 성공, 실패)
- 최소/최대 실행 시간

### 리소스별 통계
- 리소스 키 (예: 계좌번호)
- 작업 통계와 동일한 메트릭 (리소스별)

### 스레드 안전성
- 카운터용 `AtomicLong`, `AtomicInteger` 사용
- 작업/리소스 통계용 `ConcurrentHashMap`
- 최소/최대 업데이트용 `compareAndSet`

## 🧪 테스트

### 테스트 실행
```bash
./gradlew test          # 모든 테스트 실행
./gradlew check         # 테스트 및 품질 검사 실행
```

### 테스트 커버리지
- 정상 입금 성공
- 계좌 없음 시나리오
- 락 충돌 및 재시도 동작
- 트랜잭션 경계 검증
- Mockito 기반 단위 테스트

## 🎨 디자인 패턴

### 전략 패턴
플러그인 가능한 재시도 전략 허용:
- `RetryStrategy` 인터페이스
- `RandomBackoffRetryStrategy` 구현
- 새로운 전략으로 확장 용이 (지수 백오프, 서킷 브레이커 등)

### 템플릿 메서드 패턴
`LockRetryTemplate`은 재시도 로직을 캡슐화하고 비즈니스 작업은 제공된 함수에 위임합니다.

## 💡 사용 사례

### 금융 트랜잭션 시스템
- 계좌 이체 및 입금
- 고동시성 금융 작업
- 일관된 잔액 업데이트

### 재고 관리
- 재고 예약 및 업데이트
- 다중 창고 재고 조정

### 분산 시스템
- 리소스 할당 충돌
- 분산 락 관리
- 여러 서비스 간 조정

## 🔧 고급 주제

### 트랜잭션 경계 관리
프로젝트는 재시도 로직을 트랜잭션 경계와 신중하게 분리합니다:

```java
// 외부 메서드 - @Transactional 없음
public void deposit(String accountNo, BigDecimal amount) {
    lockRetryTemplate.execute(() -> depositLogic(accountNo, amount));
}

// 내부 메서드 - @Transactional 포함
@Transactional
public Void depositLogic(String accountNo, BigDecimal amount) {
    // DB 작업이 포함된 비즈니스 로직
}
```

### 예외 처리 흐름
1. 락 충돌 발생 → `PessimisticLockingFailureException`
2. `LockRetryTemplate`이 예외 캐치
3. `RetryStrategy`가 `shouldRetry()` 평가
4. true: 대기, 카운터 증가, 재시도
5. false: 원본 예외 재전파
6. Spring이 트랜잭션 롤백 마킹

## 🚨 중요 사항

- **무기한 대기 방지**: `NOWAIT`이 무한 대기 방지
- **썬더링 허드 방지**: 랜덤 백오프가 동기화된 재시도 방지
- **트랜잭션 무결성**: 각 재시도마다 새로운 트랜잭션 획득
- **프로덕션 준비**: 운영 가시성을 위한 포괄적 모니터링

## 📈 성능 고려사항

- **커넥션 풀**: 최적 동시성을 위해 설정
- **대기 시간**: 응답성과 부하의 균형 조정
- **메모리 사용량**: 메트릭용 효율적인 동시 컬렉션
- **CPU 사용량**: 재시도 로직의 최소 오버헤드

---

## 🤝 기여

이 프로젝트는 Spring Boot 애플리케이션에서 적절한 재시도 전략, 모니터링, 트랜잭션 관리를 사용하여 데이터베이스 락 충돌을 처리하는 참조 구현체 역할을 합니다.

**고동시성 시스템을 위해 ❤️로 만들었습니다**