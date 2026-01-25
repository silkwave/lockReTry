package com.example.lockretry.component;

import com.example.lockretry.domain.AccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * 분산락과 트랜잭션을 함께 관리하는 템플릿 클래스입니다.
 * Redis 분산락을 획득한 후 비즈니스 로직을 실행하고, 실행 완료 후 락을 자동으로 해제합니다.
 */
@Component
public class DistributedLockTemplate {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockTemplate.class);

    /** 분산락 관리자 */
    private final DistributedLockManager distributedLockManager;

    public DistributedLockTemplate(DistributedLockManager distributedLockManager) {
        this.distributedLockManager = distributedLockManager;
    }

    /**
     * 분산락을 사용하여 작업을 실행합니다.
     * 
     * @param <T> 작업 결과 타입
     * @param lockKey 락 키
     * @param action 실행할 작업
     * @return 작업 실행 결과
     * @throws Exception 락 획득 실패 또는 작업 실행 중 예외 발생
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, DistributedLockManager.DEFAULT_LOCK_TIMEOUT, action);
    }

    /**
     * 지정된 타임아웃으로 분산락을 사용하여 작업을 실행합니다.
     * 
     * @param <T> 작업 결과 타입
     * @param lockKey 락 키
     * @param timeout 락 만료 시간
     * @param action 실행할 작업
     * @return 작업 실행 결과
     * @throws Exception 락 획득 실패 또는 작업 실행 중 예외 발생
     */
    public <T> T executeWithLock(String lockKey, java.time.Duration timeout, Supplier<T> action) {
        try (DistributedLockManager.Lock lock = distributedLockManager.acquireLock(lockKey, timeout)) {
            if (lock == null) {
                throw new RuntimeException("분산락 획득 실패: " + lockKey);
            }
            
            log.debug("분산락 획득 완료 - 키: {}, 작업 시작", lockKey);
            T result = action.get();
            log.debug("작업 완료 - 키: {}", lockKey);
            
            return result;
        }
    }

    /**
     * 계좌 관련 작업을 위한 전용 메서드입니다.
     * 계좌 번호를 기반으로 락 키를 자동으로 생성하고 트랜잭션을 관리합니다.
     * 
     * @param <T> 작업 결과 타입
     * @param accountNo 계좌 번호
     * @param action 실행할 작업 (계좌 정보를 파라미터로 받음)
     * @return 작업 실행 결과
     * @throws Exception 락 획득 실패 또는 작업 실행 중 예외 발생
     */
    @Transactional
    public <T> T executeWithAccountLock(String accountNo, java.util.function.Function<AccountDto, T> action) {
        String lockKey = "account:update:" + accountNo;
        
        return executeWithLock(lockKey, () -> {
            // 실제 비즈니스 로직은 별도 서비스에서 처리
            // 여기서는 계좌 락만 관리
            throw new UnsupportedOperationException("이 메서드는 AccountService에서 구현되어야 합니다");
        });
    }

    /**
     * 대기 시간을 지정하여 분산락을 획득하고 작업을 실행합니다.
     * 락이 바로 획득되지 않을 경우 지정된 시간 동안 대기합니다.
     * 
     * @param <T> 작업 결과 타입
     * @param lockKey 락 키
     * @param waitTime 락 획득 대기 시간
     * @param lockTimeout 락 만료 시간
     * @param action 실행할 작업
     * @return 작업 실행 결과
     * @throws Exception 락 획득 타임아웃 또는 작업 실행 중 예외 발생
     */
    public <T> T executeWithLockAndWait(String lockKey, java.time.Duration waitTime, 
                                       java.time.Duration lockTimeout, Supplier<T> action) {
        try (DistributedLockManager.Lock lock = distributedLockManager.tryLockWithTimeout(
                lockKey, waitTime, lockTimeout)) {
            
            if (lock == null) {
                throw new RuntimeException(
                    String.format("분산락 획득 타임아웃: %s (대기 시간: %s)", lockKey, waitTime)
                );
            }
            
            log.debug("분산락 획득 완료 (대기 후) - 키: {}, 작업 시작", lockKey);
            T result = action.get();
            log.debug("작업 완료 - 키: {}", lockKey);
            
            return result;
        }
    }
}