package com.example.lockretry.component;

import com.example.lockretry.strategy.RetryStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 데이터베이스 락 충돌 시 재시도 로직을 처리하는 템플릿 클래스입니다.
 * Strategy Pattern을 사용하여 다양한 재시도 정책을 유연하게 적용할 수 있도록 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockRetryTemplate {

    /** 재시도 정책을 정의하는 인터페이스 */
    private final RetryStrategy retryStrategy;

    /**
     * 락 충돌 발생 가능성이 있는 작업을 재시도 전략에 따라 실행합니다.
     *
     * @param action 실행할 비즈니스 로직 (람다 형태)
     * @param <T> 반환 타입
     * @return 작업 실행 결과
     * @throws Exception 재시도 횟수 초과 또는 재시도 대상이 아닌 예외 발생 시
     */
    public <T> T execute(Supplier<T> action) {
        int attempt = 0; // 현재 시도 횟수
        while (true) { // 무한 루프를 통해 재시도
            try {
                T result = action.get(); // 비즈니스 로직 실행
                if (attempt > 0) {
                    log.info("작업 성공 (재시도 횟수: {})", attempt);
                }
                return result;
            } catch (Exception e) {
                // 재시도 전략에 따라 재시도 여부 결정
                if (retryStrategy.shouldRetry(e, attempt)) {
                    attempt++; // 시도 횟수 증가
                    long waitTime = retryStrategy.getWaitTime(attempt); // 다음 재시도까지 대기 시간 계산
                    log.warn("락 충돌 감지 (ORA-00054). {}/{} 번째 재시도를 {}ms 후에 수행합니다...",
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
}