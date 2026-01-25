package com.example.lockretry.strategy;

import org.springframework.stereotype.Component;

/**
 * 고정 대기 시간(Fixed Delay) 재시도 전략 구현체입니다.
 * 실패 시 항상 동일한 시간만큼 대기한 후 재시도합니다.
 * 간단하지만 예측 가능한 재시도 패턴이 필요할 때 유용합니다.
 */
@Component
public class FixedDelayRetryStrategy implements RetryStrategy {

    /** 최대 재시도 횟수 */
    private final int maxRetries;
    
    /** 고정 대기 시간 (밀리초) */
    private final long fixedDelayMs;

    /**
     * 기본 설정값으로 고정 대기 시간 전략을 생성합니다.
     */
    public FixedDelayRetryStrategy() {
        this(10, 100); // 최대 10회 재시도, 100ms 대기
    }

    /**
     * 지정된 설정값으로 고정 대기 시간 전략을 생성합니다.
     * 
     * @param maxRetries 최대 재시도 횟수
     * @param fixedDelayMs 고정 대기 시간 (밀리초)
     */
    public FixedDelayRetryStrategy(int maxRetries, long fixedDelayMs) {
        this.maxRetries = maxRetries;
        this.fixedDelayMs = fixedDelayMs;
        
        System.out.println(String.format("고정 대기 시간 재시도 전략 초기화 - 최대 재시도: %d회, 대기 시간: %dms", 
                                        maxRetries, fixedDelayMs));
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
        // 항상 동일한 대기 시간 반환
        return fixedDelayMs;
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