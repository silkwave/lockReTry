package com.example.lockretry.strategy;

public interface RetryStrategy {
    /**
     * 예외 종류와 현재 시도 횟수를 기반으로 재시도 여부를 결정합니다.
     */
    boolean shouldRetry(Exception e, int attemptCount);

    /**
     * 다음 시도까지 대기할 시간(ms)을 반환합니다.
     */
    long getWaitTime(int attemptCount);
}