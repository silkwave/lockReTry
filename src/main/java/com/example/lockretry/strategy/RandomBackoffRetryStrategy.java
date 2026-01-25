package com.example.lockretry.strategy;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 랜덤 백오프(Random Backoff) 전략을 사용하여 재시도 정책을 구현한 클래스입니다.
 * 락 획득 실패 시, 정해진 최대 재시도 횟수까지 랜덤한 시간만큼 대기 후 재시도합니다.
 * 이를 통해 동시성 충돌 발생 시 여러 트랜잭션이 동시에 재시도하여 다시 충돌하는 현상을 줄입니다.
 */
@Slf4j
@Component
public class RandomBackoffRetryStrategy implements RetryStrategy {

    /** 최대 재시도 횟수 */
    private static final int MAX_RETRIES = 10;
    /** 재시도 간 기본 대기 시간 (밀리초) */
    private static final long BASE_WAIT_TIME_MS = 100; // 기본 대기 0.1초
    /** 기본 대기 시간에 추가될 랜덤 지터(Jitter)의 최대값 (밀리초) */
    private static final long MAX_JITTER_MS = 200; // 랜덤 추가 대기 최대 0.2초
    /** 최대 대기 시간 (밀리초) */
    private static final long MAX_WAIT_TIME_MS = 2000; // 2초

    @Override
    public boolean shouldRetry(Exception e, int attemptCount) {

        log.warn("랜덤 백오프 shouldRetry... attempt: {}, msg: {}", attemptCount, e.getMessage());

        // 최대 재시도 횟수 초과 시 중단
        if (attemptCount >= MAX_RETRIES) {
            return false;
        }

        // 락 획득 실패 예외이거나, 에러 메시지에서 락 충돌이 감지되면 재시도
        return e instanceof PessimisticLockingFailureException || isLockConflict(e);
    }

    /**
     * 데이터베이스 락 충돌 여부를 판단합니다.
     * 다양한 데이터베이스의 락 관련 에러 메시지를 감지합니다.
     *
     * @param e 발생한 예외
     * @return 락 충돌이면 true, 그렇지 않으면 false
     */
    private boolean isLockConflict(Exception e) {
        // Oracle ORA-00054, H2 데이터베이스 락 메시지 확인
        String message = e.getMessage() != null ? e.getMessage() : "";
        return message.contains("ORA-00054")
                || message.contains("Timeout trying to lock table")
                || message.contains("Lock timeout")
                || message.contains("busy")
                || (e.getCause() != null &&
                        e.getCause().getMessage() != null &&
                        e.getCause().getMessage().contains("lock"));
    }

    @Override
    public long getWaitTime(int attemptCount) {
        log.warn("지수 백오프 getWaitTime... attempt: {}", attemptCount);
        return calculateBackoffTime(attemptCount);
    }

    /**
     * 지수 백오프 방식으로 대기 시간을 계산합니다.
     * 시도 횟수가 많아질수록 대기 시간이 증가하지만, MAX_WAIT_TIME_MS를 넘지 않습니다.
     *
     * @param attempt 현재 시도 횟수
     * @return 대기 시간 (밀리초)
     */
    private long calculateBackoffTime(int attempt) {
        // 기본: 100ms + (attempt * 50ms) + 랜덤값(0~200ms)
        long baseWait = BASE_WAIT_TIME_MS + (attempt * 50L);
        long randomWait = ThreadLocalRandom.current().nextLong(MAX_JITTER_MS);
        return Math.min(baseWait + randomWait, MAX_WAIT_TIME_MS);
    }
}