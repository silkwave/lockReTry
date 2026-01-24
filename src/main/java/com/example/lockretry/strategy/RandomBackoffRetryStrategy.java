package com.example.lockretry.strategy;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomBackoffRetryStrategy implements RetryStrategy {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_WAIT_TIME_MS = 100; // 기본 대기 0.1초
    private static final long MAX_JITTER_MS = 200;     // 랜덤 추가 대기 최대 0.2초

    @Override
    public boolean shouldRetry(Exception e, int attemptCount) {
        // 락 획득 실패 예외이면서, 최대 재시도 횟수 미만인 경우 true
        if (e instanceof PessimisticLockingFailureException) {
            return attemptCount < MAX_RETRIES;
        }
        return false;
    }

    @Override
    public long getWaitTime(int attemptCount) {
        // Random Backoff: 기본 대기 시간 + 랜덤 지터 (충돌 분산)
        return BASE_WAIT_TIME_MS + ThreadLocalRandom.current().nextLong(MAX_JITTER_MS);
    }
}