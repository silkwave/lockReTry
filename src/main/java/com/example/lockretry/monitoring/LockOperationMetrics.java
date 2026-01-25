package com.example.lockretry.monitoring;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 락 및 재시도 관련 작업의 실행 통계를 저장하는 데이터 클래스입니다.
 * 성능 모니터링 및 장애 분석을 위해 사용됩니다.
 */
@Data
public class LockOperationMetrics {

    /** 작업 유형 (예: DEPOSIT, WITHDRAW, TRANSFER) */
    private String operationType;
    
    /** 리소스 키 (예: 계좌 번호) */
    private String resourceKey;
    
    /** 작업 시작 시간 */
    private LocalDateTime startTime;
    
    /** 작업 완료 시간 */
    private LocalDateTime endTime;
    
    /** 총 재시도 횟수 */
    private AtomicInteger retryCount = new AtomicInteger(0);
    
    /** 총 대기 시간 (밀리초) */
    private AtomicLong totalWaitTimeMs = new AtomicLong(0);
    
    /** 락 획득에 걸린 시간 (밀리초) */
    private long lockAcquisitionTimeMs;
    
    /** 작업 성공 여부 */
    private boolean success;
    
    /** 실패 원인 (실패 시) */
    private String failureReason;
    
    /** 마지막 예외 (실패 시) */
    private Exception lastException;

    /**
     * 작업 시작 시간을 기록합니다.
     */
    public void start() {
        this.startTime = LocalDateTime.now();
        this.retryCount.set(0);
        this.totalWaitTimeMs.set(0);
    }

    /**
     * 재시도 횟수와 대기 시간을 기록합니다.
     * 
     * @param waitTimeMs 대기 시간 (밀리초)
     */
    public void recordRetry(long waitTimeMs) {
        this.retryCount.incrementAndGet();
        this.totalWaitTimeMs.addAndGet(waitTimeMs);
    }

    // Getter 메서드들 추가
    public AtomicInteger getRetryCount() {
        return retryCount;
    }

    public AtomicLong getTotalWaitTimeMs() {
        return totalWaitTimeMs;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    /**
     * 작업 완료 시간을 기록하고 성공으로 처리합니다.
     */
    public void recordSuccess() {
        this.endTime = LocalDateTime.now();
        this.success = true;
    }

    /**
     * 작업 완료 시간을 기록하고 실패로 처리합니다.
     * 
     * @param failureReason 실패 원인
     * @param exception 발생한 예외
     */
    public void recordFailure(String failureReason, Exception exception) {
        this.endTime = LocalDateTime.now();
        this.success = false;
        this.failureReason = failureReason;
        this.lastException = exception;
    }

    /**
     * 총 실행 시간을 계산합니다.
     * 
     * @return 총 실행 시간 (밀리초)
     */
    public long getTotalExecutionTimeMs() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMillis();
    }

    /**
     * 평균 대기 시간을 계산합니다.
     * 
     * @return 평균 대기 시간 (밀리초)
     */
    public long getAverageWaitTimeMs() {
        int retries = retryCount.get();
        return retries > 0 ? totalWaitTimeMs.get() / retries : 0;
    }

    /**
     * 현재까지의 진행 상황 요약 정보를 반환합니다.
     * 
     * @return 진행 상황 문자열
     */
    public String getProgressSummary() {
        return String.format("작업: %s, 리소스: %s, 재시도: %d, 대기: %dms, 경과: %dms", 
                           operationType, resourceKey, retryCount.get(), 
                           totalWaitTimeMs.get(), getElapsedTimeMs());
    }

    /**
     * 현재까지 경과된 시간을 계산합니다.
     * 
     * @return 경과 시간 (밀리초)
     */
    private long getElapsedTimeMs() {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        return java.time.Duration.between(startTime, now).toMillis();
    }
}