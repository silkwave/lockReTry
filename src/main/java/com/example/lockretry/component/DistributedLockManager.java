package com.example.lockretry.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 분산락 컴포넌트
 */
@Component
public class DistributedLockManager {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockManager.class);

    private final StringRedisTemplate redisTemplate;

    /** 기본 락 만료 시간 */
    public static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofSeconds(30);

    /** 락 재시도 간격 */
    private static final long LOCK_RETRY_INTERVAL_MS = 100;

    public DistributedLockManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Lock acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_LOCK_TIMEOUT);
    }

    public Lock acquireLock(String lockKey, Duration timeout) {
        // 🔥 timeout null 제거 (IDE 경고 핵심 해결)
        Duration effectiveTimeout =
                timeout != null ? timeout : DEFAULT_LOCK_TIMEOUT;

        String redisKey = "lock:" + lockKey;
        String lockValue = UUID.randomUUID().toString();

        log.debug("분산락 획득 시도 - key={}, timeout={}", redisKey, effectiveTimeout);

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                redisKey,
                lockValue,
                effectiveTimeout.toMillis(),
                TimeUnit.MILLISECONDS
        );

        if (Boolean.TRUE.equals(acquired)) {
            log.info("분산락 획득 성공 - key={}", redisKey);
            return new Lock(redisKey, lockValue, effectiveTimeout);
        }

        log.warn("분산락 획득 실패 - key={}", redisKey);
        return null;
    }

    /**
     * 분산락 객체
     */
    public class Lock implements AutoCloseable {

        private final String redisKey;
        private final String lockValue;
        private final Duration timeout;

        private volatile boolean released = false;

        public Lock(String redisKey, String lockValue, Duration timeout) {
            // 🔥 Null 완전 차단
            this.redisKey = Objects.requireNonNull(redisKey);
            this.lockValue = Objects.requireNonNull(lockValue);
            this.timeout = Objects.requireNonNull(timeout);
        }

        /**
         * 락 해제
         */
        public boolean release() {
            if (released) {
                log.warn("이미 해제된 락 - key={}", redisKey);
                return false;
            }

            String currentValue = redisTemplate.opsForValue().get(Objects.requireNonNull(redisKey));
            if (currentValue == null) {
                log.warn("락 해제 실패 - Redis 키 없음: {}", redisKey);
                return false;
            }

            if (!lockValue.equals(currentValue)) {
                log.error("락 해제 실패 - 값 불일치 (stored={}, request={})",
                        currentValue, lockValue);
                return false;
            }

            Boolean deleted = redisTemplate.delete(Objects.requireNonNull(redisKey));
            released = Boolean.TRUE.equals(deleted);
            return released;
        }

        /**
         * 락 만료 시간 연장
         */
        public boolean renew() {
            if (released) {
                return false;
            }

            String currentValue = redisTemplate.opsForValue().get(Objects.requireNonNull(redisKey));
            if (currentValue == null || !lockValue.equals(currentValue)) {
                log.warn("락 연장 실패 - 값 불일치 또는 키 없음");
                return false;
            }

            Boolean renewed = redisTemplate.expire(Objects.requireNonNull(redisKey), Objects.requireNonNull(timeout));
            if (Boolean.TRUE.equals(renewed)) {
                log.debug("락 만료 시간 연장 성공 - key={}", redisKey);
                return true;
            }

            log.warn("락 만료 시간 연장 실패 - key={}", redisKey);
            return false;
        }

        @Override
        public void close() {
            release();
        }

        public boolean isValid() {
            return !released;
        }
    }

    /**
     * 지정 시간 동안 락 획득 시도
     */
    public Lock tryLockWithTimeout(
            String lockKey,
            Duration waitTime,
            Duration lockTimeout
    ) {
        long deadline = System.currentTimeMillis() + waitTime.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Lock lock = acquireLock(lockKey, lockTimeout);
            if (lock != null) {
                return lock;
            }

            try {
                Thread.sleep(LOCK_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("락 대기 중 인터럽트 발생 - key={}", lockKey);
                return null;
            }
        }

        log.warn("락 획득 타임아웃 - key={}, waitTime={}", lockKey, waitTime);
        return null;
    }
}
