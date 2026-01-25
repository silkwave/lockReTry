package com.example.lockretry.template;

import com.example.lockretry.strategy.RetryStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * 데이터베이스 락 충돌 시 재시도 로직을 처리하는 추상 템플릿 클래스입니다.
 * 재시도 전략(RetryStrategy)을 사용하여 다양한 재시도 정책을 유연하게 적용할 수 있도록 합니다.
 * 구체적인 비즈니스 로직은 doInLockedSection() 추상 메서드를 통해 하위 클래스에서 구현합니다.
 */
@Slf4j
public abstract class AbstractLockRetryTemplate<T> {

    /** 재시도 정책을 정의하는 인터페이스 */
    private final RetryStrategy retryStrategy;

    protected AbstractLockRetryTemplate(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    /**
     * 락 충돌 발생 가능성이 있는 작업을 재시도 전략에 따라 실행합니다.
     * 이 메서드는 알고리즘의 골격을 정의하며, 실제 비즈니스 로직은 doInLockedSection() 메서드에 위임합니다.
     *
     * @return 작업 실행 결과
     * @throws Exception 재시도 횟수 초과 또는 재시도 대상이 아닌 예외 발생 시
     */
    public final T execute() {
        int attempt = 0; // 현재 시도 횟수
        while (true) { // 무한 루프를 통해 재시도
            try {
                T result = doInLockedSection(); // 하위 클래스에서 구현할 비즈니스 로직 실행
                if (attempt > 0) {
                    log.error("작업 성공 (재시도 횟수: {})", attempt);
                }
                return result;
            } catch (Exception e) {
                // 재시도 전략에 따라 재시도 여부 결정
                if (retryStrategy.shouldRetry(e, attempt)) {
                    attempt++; // 시도 횟수 증가
                    long waitTime = retryStrategy.getWaitTime(attempt); // 다음 재시도까지 대기 시간 계산
                    log.error("락 충돌 감지 (ORA-00054). {}/{} 번째 재시도를 {}ms 후에 수행합니다...",
                            attempt, 3, waitTime); // 로그 메시지 수정 (MAX_RETRIES는 RandomBackoffRetryStrategy에서 관리되므로 상수 대신 메시지에 3을 명시)
                    
                    try {
                        Thread.sleep(waitTime); // 지정된 시간만큼 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                        throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                    }
                } else {
                    log.error("재시도 중단. 시도 횟수: {}, 원인: {}", attempt, e.getMessage());
                    throw new RuntimeException("재시도 대상이 아니거나 최대 재시도 횟수 초과 시 예외 전파", e);
                }
            }
        }
    }

    /**
     * 락 획득을 시도하며 재시도되어야 할 실제 비즈니스 로직을 정의합니다.
     * 이 메서드는 하위 클래스에서 반드시 구현해야 합니다.
     *
     * @return 락 획득 후 수행된 비즈니스 로직의 결과
     */
    protected abstract T doInLockedSection();
}