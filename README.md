# 분산락 및 재시도 라이브러리

## 개요

이 프로젝트는 분산 환경에서의 동시성 제어와 락 충돌 처리를 위한 Spring Boot 기반 라이브러리입니다. 데이터베이스 락과 Redis 분산락을 결합하여 안정적인 동시성 관리를 제공하며, 다양한 재시도 전략을 지원합니다.

## 주요 기능

### 🔒 분산락 관리
- **Redis 기반 분산락**: 여러 서버 인스턴스 간의 락 동기화
- **자동 락 해제**: try-with-resources 구문을 통한 안전한 락 관리
- **락 만료시간 연장**: 장기 실행 작업을 위한 락 타임아웃 자동 연장
- **락 획득 대기**: 지정된 시간 동안 락 획득을 시도하는 기능

### 🔄 다양한 재시도 전략
- **랜덤 백오프 (Random Backoff)**: 랜덤한 대기 시간으로 동시성 충돌 감소
- **지수 백오프 (Exponential Backoff)**: 시도 횟수에 비례하여 대기 시간 증가
- **고정 대기 시간 (Fixed Delay)**: 항상 동일한 대기 시간으로 예측 가능한 재시도

### 📊 실시간 모니터링
- **성능 통계**: 작업 실행 시간, 성공률, 평균 재시도 횟수
- **작업 유형별 통계**: 각 작업 유형의 성능 및 안정성 분석
- **리소스별 통계**: 특정 리소스에 대한 집중적인 모니터링
- **실시간 메트릭**: 작업 진행 상황 및 장애 상황 즉시 파악

## 프로젝트 구조

```
src/main/java/com/example/lockretry/
├── component/                    # 핵심 컴포넌트
│   ├── DistributedLockManager.java      # 분산락 관리자
│   ├── DistributedLockTemplate.java     # 분산락 템플릿
│   └── LockRetryTemplate.java           # 락 재시도 템플릿
├── config/                      # 설정 관리
│   ├── RetryStrategyConfig.java         # 재시도 정책 설정
│   └── TransactionConfig.java           # 트랜잭션 설정
├── controller/                  # REST API 컨트롤러
│   ├── AccountController.java            # 계좌 관련 API
│   └── AccountQueryController.java       # 계좌 조회 API
├── domain/                      # 데이터 모델
│   └── AccountDto.java                   # 계좌 정보 DTO
├── mapper/                      # 데이터베이스 매퍼
│   ├── AccountMapper.java                # 계좌 CRUD 매퍼
│   └── AccountQueryMapper.java           # 계좌 조회 매퍼
├── monitoring/                  # 모니터링 컴포넌트
│   ├── LockMetricsCollector.java         # 통계 수집기
│   ├── LockOperationMetrics.java         # 작업 메트릭
│   ├── OperationStats.java               # 작업 유형별 통계
│   └── ResourceStats.java               # 리소스별 통계
├── service/                     # 비즈니스 로직
│   └── AccountService.java               # 계좌 관련 서비스
├── strategy/                    # 재시도 전략
│   ├── RetryStrategy.java               # 재시도 전략 인터페이스
│   ├── RandomBackoffRetryStrategy.java  # 랜덤 백오프 전략
│   ├── ExponentialBackoffRetryStrategy.java # 지수 백오프 전략
│   └── FixedDelayRetryStrategy.java     # 고정 대기 시간 전략
└── LockRetryApplication.java            # 메인 애플리케이션 클래스
```

## 시작하기

### 전제 조건
- Java 11 이상
- Spring Boot 2.7 이상
- Redis 서버 (분산락 기능 사용 시)
- 데이터베이스 (Oracle, MySQL, H2 등)

### 설정 파일

`application.yml` 예시:

```yaml
# 재시도 전략 설정
retry:
  strategy:
    default-strategy: RANDOM_BACKOFF  # 기본 전략: FIXED_DELAY, RANDOM_BACKOFF, EXPONENTIAL_BACKOFF
    max-retries: 10                   # 최대 재시도 횟수
    base-delay-ms: 100                # 기본 대기 시간 (밀리초)
    max-delay-ms: 2000                # 최대 대기 시간 (밀리초)
    backoff-multiplier: 2.0           # 지수 백오프 배수
    max-jitter-ms: 200                # 랜덤 지터 최대값 (밀리초)

# Redis 설정 (분산락 사용 시)
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms

# 데이터베이스 설정
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
```

## 사용 예제

### 1. 기본적인 분산락 사용

```java
@Service
@RequiredArgsConstructor
public class ExampleService {
    
    private final DistributedLockManager lockManager;
    
    public void performCriticalOperation(String resourceId) {
        String lockKey = "operation:" + resourceId;
        
        // try-with-resources로 자동 락 해제
        try (DistributedLockManager.Lock lock = lockManager.acquireLock(lockKey)) {
            if (lock == null) {
                throw new RuntimeException("락 획득 실패: " + lockKey);
            }
            
            // 중요한 비즈니스 로직 수행
            System.out.println("락 획득 성공, 작업 수행 중...");
            
        } // 락 자동 해제
    }
}
```

### 2. 재시도 템플릿 사용

```java
@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final LockRetryTemplate retryTemplate;
    private final AccountMapper accountMapper;
    
    public void deposit(String accountNo, BigDecimal amount) {
        retryTemplate.execute(() -> {
            // 락 충돌 발생 가능성이 있는 작업
            return depositLogic(accountNo, amount);
        });
    }
    
    @Transactional
    private Void depositLogic(String accountNo, BigDecimal amount) {
        // 락 획득 및 비즈니스 로직
        AccountDto account = accountMapper.selectAccountForUpdate(accountNo);
        account.setBalance(account.getBalance().add(amount));
        accountMapper.updateBalance(account);
        return null;
    }
}
```

### 3. 분산락 템플릿 사용

```java
@Service
@RequiredArgsConstructor
public class DistributedService {
    
    private final DistributedLockTemplate lockTemplate;
    
    public void processWithLock(String resourceKey) {
        String lockKey = "process:" + resourceKey;
        
        lockTemplate.executeWithLock(lockKey, () -> {
            // 분산락으로 보호되는 작업
            System.out.println("분산락 하에서 작업 수행");
            return "작업 완료";
        });
    }
}
```

### 4. 모니터링 기능 활용

```java
@RestController
@RequiredArgsConstructor
public class MonitoringController {
    
    private final LockMetricsCollector metricsCollector;
    
    @GetMapping("/api/metrics")
    public ResponseEntity<String> getMetrics() {
        var statistics = metricsCollector.getOverallStatistics();
        return ResponseEntity.ok(statistics.toString());
    }
    
    @GetMapping("/api/metrics/operation/{type}")
    public ResponseEntity<String> getOperationMetrics(@PathVariable String type) {
        var stats = metricsCollector.getOperationStatistics(type);
        return ResponseEntity.ok(stats.toString());
    }
    
    @GetMapping("/api/metrics/resource/{key}")
    public ResponseEntity<String> getResourceMetrics(@PathVariable String key) {
        var stats = metricsCollector.getResourceStatistics(key);
        return ResponseEntity.ok(stats.toString());
    }
}
```

## 재시도 전략 상세

### 랜덤 백오프 (Random Backoff)
- **특징**: 기본 대기 시간 + 랜덤 지터 추가
- **용도**: 여러 클라이언트가 동시에 재시도할 때 충돌 방지
- **예시**: 100ms + (시도횟수 × 50ms) + 0~200ms 랜덤값

### 지수 백오프 (Exponential Backoff)
- **특징**: 시도 횟수에 따라 대기 시간이 지수적으로 증가
- **용도**: 네트워크 혼잡이나 일시적인 장애 상황
- **예시**: 100ms × 2^n (최대 2000ms)

### 고정 대기 시간 (Fixed Delay)
- **특징**: 항상 동일한 대기 시간
- **용도**: 예측 가능하고 단순한 재시도 패턴 필요 시
- **예시**: 항상 100ms 대기

## 모니터링 지표

### 전체 통계
- 총 작업 실행 횟수
- 성공/실패 작업 수
- 전체 성공률
- 평균 재시도 횟수
- 평균 대기 시간

### 작업 유형별 통계
- 각 작업 유형의 성능 지표
- 최소/최대 실행 시간
- 작업 유형별 성공률

### 리소스별 통계
- 특정 리소스(계좌 등)에 대한 집중 모니터링
- 리소스별 경합도 분석
- 병목 현상 식별

## API 명세

### 계좌 관리 API

#### 입금
```
POST /api/deposit
Parameters:
- accountNo: 계좌번호 (String)
- amount: 입금액 (BigDecimal)

Response:
- 200: 입금 성공 메시지
- 500: 입금 실패 메시지
```

### 모니터링 API

#### 전체 통계
```
GET /api/metrics
Response: 전체 통계 정보
```

#### 작업 유형별 통계
```
GET /api/metrics/operation/{type}
Response: 작업 유형별 통계 정보
```

#### 리소스별 통계
```
GET /api/metrics/resource/{key}
Response: 리소스별 통계 정보
```

## 주의사항

1. **Redis 연결**: 분산락 기능을 사용하려면 Redis 서버가 필수입니다.
2. **락 타임아웃**: 락 만료 시간은 작업의 예상 실행 시간보다 충분히 길게 설정하세요.
3. **트랜잭션**: 데이터베이스 락과 분산락을 함께 사용할 때 트랜잭션 범위를 신중하게 관리하세요.
4. **모니터링**: 정기적으로 통계를 확인하여 성능 저하나 병목 현상을 조기에 발견하세요.

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 기여

이 프로젝트에 기여하고 싶으시다면 다음 단계를 따르세요:
1. 이 리포지토리를 포크합니다.
2. 기능 브랜치를 생성합니다.
3. 변경 사항을 커밋합니다.
4. 풀 리퀘스트를 제출합니다.

## 지원

문제가 발생하거나 질문이 있으시면 GitHub Issues를 통해 문의해 주세요.