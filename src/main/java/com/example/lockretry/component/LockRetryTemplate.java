package com.example.lockretry.component;

import com.example.lockretry.strategy.RetryStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class LockRetryTemplate {

    private final RetryStrategy retryStrategy;

    public <T> T execute(Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (Exception e) {
                if (retryStrategy.shouldRetry(e, attempt)) {
                    attempt++;
                    long waitTime = retryStrategy.getWaitTime(attempt);
                    log.warn("Lock conflict detected (ORA-00054). Retrying attempt {}/{} after {}ms...", 
                            attempt, 3, waitTime);
                    
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    throw e; // 재시도 대상이 아니거나 횟수 초과 시 예외 전파
                }
            }
        }
    }
}