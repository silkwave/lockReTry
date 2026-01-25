package com.example.lockretry.strategy;

import org.springframework.stereotype.Component;

/**
 * 지수 백오프(Exponential Backoff) 재시도 전략 구현체입니다.
 * 실패 시 시도 횟수에 비례하여 점점 증가하는 대기 시간을 가집니다.
 * 네트워크 혼잡이나 일시적인 장애 상황에서 효과적인 재시도 패턴을 제공합니다.
 */
@Component
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    /** 최대 재시도 횟수 */
    private final int maxRetries;
    
    /** 기본 대기 시간 (밀리초) */
    private final long baseDelayMs;
    
    /** 최대 대기 시간 (밀리초) */
    private final long maxDelayMs;
    
    /** 지수 백오프 배수 */
    private final double backoffMultiplier;

    /**
     * 기본 설정값으로 지수 백오프 전략을 생성합니다.
     */
    public ExponentialBackoffRetryStrategy() {
        this(10, 100, 2000, 2.0); // 최대 10회, 100ms 기본, 최대 2초, 2배 증가
    }

    /**
     * 지정된 설정값으로 지수 백오프 전략을 생성합니다.
     * 
     * @param maxRetries 최대 재시도 횟수
     * @param baseDelayMs 기본 대기 시간 (밀리초)
     * @param maxDelayMs 최대 대기 시간 (밀리초)
     * @param backoffMultiplier 지수 백오프 배수
     */
    public ExponentialBackoffRetryStrategy(int maxRetries, long baseDelayMs, 
                                          long maxDelayMs, double backoffMultiplier) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        
        System.out.println(String.format("지수 백오프 재시도 전략 초기화 - 최대 재시도: %d회, 기본 대기: %dms, 최대 대기: %dms, 배수: %.1f", 
                                        maxRetries, baseDelayMs, maxDelayMs, backoffMultiplier));
    }

    @Override
    public boolean shouldRetry(Exception e, int attemptCount) {
        // 최대 재시도 횟수 초과 시 중단
        if (attemptCount >= maxRetries) {
            return false;
        }

        // 락 관련 예외인지 확인
        return isLockConflict(e);
    }

    @Override
    public long getWaitTime(int attemptCount) {
        // 지수 백오프 공식: baseDelay * (multiplier ^ attemptCount)
        // 최대 대기 시간을 초과하지 않도록 제한
        long exponentialDelay = (long) (baseDelayMs * Math.pow(backoffMultiplier, attemptCount));
        return Math.min(exponentialDelay, maxDelayMs);
    }

    /**
     * 데이터베이스 락 충돌 여부를 판단합니다.
     * 
     * @param e 발생한 예외
     * @return 락 충돌이면 true, 그렇지 않으면 false
     */
    private boolean isLockConflict(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "";
        return message.contains("ORA-00054")
                || message.contains("Timeout trying to lock table")
                || message.contains("Lock timeout")
                || message.contains("busy")
                || (e.getCause() != null &&
                        e.getCause().getMessage() != null &&
                        e.getCause().getMessage().contains("lock"));
    }
}