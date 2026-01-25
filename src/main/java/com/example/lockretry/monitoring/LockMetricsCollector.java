package com.example.lockretry.monitoring;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 락 및 재시도 작업의 통계 정보를 관리하고 수집하는 모니터링 컴포넌트입니다.
 * 실시간 성능 모니터링 및 통계 분석 기능을 제공합니다.
 */
@Component
public class LockMetricsCollector {

    /** 총 작업 실행 횟수 */
    private final AtomicLong totalOperations = new AtomicLong(0);
    
    /** 성공한 작업 횟수 */
    private final AtomicLong successfulOperations = new AtomicLong(0);
    
    /** 실패한 작업 횟수 */
    private final AtomicLong failedOperations = new AtomicLong(0);
    
    /** 총 재시도 횟수 */
    private final AtomicLong totalRetries = new AtomicLong(0);
    
    /** 총 대기 시간 (밀리초) */
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    
    /** 작업 유형별 통계 */
    private final ConcurrentHashMap<String, OperationStats> operationStatsMap = new ConcurrentHashMap<>();
    
    /** 리소스별 통계 */
    private final ConcurrentHashMap<String, ResourceStats> resourceStatsMap = new ConcurrentHashMap<>();

    /**
     * 새로운 작업 시작을 기록합니다.
     * 
     * @param operationType 작업 유형
     * @param resourceKey 리소스 키
     * @return 생성된 메트릭 객체
     */
    public LockOperationMetrics startOperation(String operationType, String resourceKey) {
        totalOperations.incrementAndGet();
        
        LockOperationMetrics metrics = new LockOperationMetrics();
        metrics.setOperationType(operationType);
        metrics.setResourceKey(resourceKey);
        metrics.start();
        
        System.out.println(String.format("작업 시작 - 유형: %s, 리소스: %s, 누적 작업: %d", 
                                        operationType, resourceKey, totalOperations.get()));
        
        return metrics;
    }

    /**
     * 작업 성공 완료를 기록합니다.
     * 
     * @param metrics 작업 메트릭
     */
    public void recordSuccess(LockOperationMetrics metrics) {
        metrics.recordSuccess();
        successfulOperations.incrementAndGet();
        
        // 작업 유형별 통계 업데이트
        updateOperationStats(metrics, true);
        
        // 리소스별 통계 업데이트
        updateResourceStats(metrics, true);
        
        System.out.println(String.format("작업 성공 - %s, 총 성공: %d, 실행시간: %dms", 
                                        metrics.getProgressSummary(), 
                                        successfulOperations.get(),
                                        metrics.getTotalExecutionTimeMs()));
    }

    /**
     * 작업 실패를 기록합니다.
     * 
     * @param metrics 작업 메트릭
     * @param failureReason 실패 원인
     * @param exception 발생한 예외
     */
    public void recordFailure(LockOperationMetrics metrics, String failureReason, Exception exception) {
        metrics.recordFailure(failureReason, exception);
        failedOperations.incrementAndGet();
        
        // 재시도 횟수와 대기 시간 누적
        totalRetries.addAndGet(metrics.getRetryCount().get());
        totalWaitTime.addAndGet(metrics.getTotalWaitTimeMs().get());
        
        // 작업 유형별 통계 업데이트
        updateOperationStats(metrics, false);
        
        // 리소스별 통계 업데이트
        updateResourceStats(metrics, false);
        
        System.out.println(String.format("작업 실패 - %s, 실패 원인: %s, 총 실패: %d", 
                                        metrics.getProgressSummary(), 
                                        failureReason,
                                        failedOperations.get()));
    }

    /**
     * 현재까지의 전체 통계 정보를 반환합니다.
     * 
     * @return 전체 통계 요약
     */
    public StatisticsSummary getOverallStatistics() {
        StatisticsSummary summary = new StatisticsSummary();
        summary.setTotalOperations(totalOperations.get());
        summary.setSuccessfulOperations(successfulOperations.get());
        summary.setFailedOperations(failedOperations.get());
        summary.setTotalRetries(totalRetries.get());
        summary.setTotalWaitTime(totalWaitTime.get());
        summary.setSuccessRate(calculateSuccessRate());
        summary.setAverageRetries(calculateAverageRetries());
        summary.setAverageWaitTime(calculateAverageWaitTime());
        summary.setGeneratedAt(LocalDateTime.now());
        
        return summary;
    }

    /**
     * 작업 유형별 통계 정보를 반환합니다.
     * 
     * @param operationType 작업 유형
     * @return 작업 유형별 통계
     */
    public OperationStats getOperationStatistics(String operationType) {
        return operationStatsMap.getOrDefault(operationType, new OperationStats(operationType));
    }

    /**
     * 리소스별 통계 정보를 반환합니다.
     * 
     * @param resourceKey 리소스 키
     * @return 리소스별 통계
     */
    public ResourceStats getResourceStatistics(String resourceKey) {
        return resourceStatsMap.getOrDefault(resourceKey, new ResourceStats(resourceKey));
    }

    /**
     * 모든 통계를 초기화합니다.
     */
    public void resetStatistics() {
        totalOperations.set(0);
        successfulOperations.set(0);
        failedOperations.set(0);
        totalRetries.set(0);
        totalWaitTime.set(0);
        operationStatsMap.clear();
        resourceStatsMap.clear();
        
        System.out.println("모든 통계 정보가 초기화되었습니다.");
    }

    /**
     * 작업 유형별 통계를 업데이트합니다.
     */
    private void updateOperationStats(LockOperationMetrics metrics, boolean success) {
        operationStatsMap.compute(metrics.getOperationType(), (key, stats) -> {
            if (stats == null) {
                stats = new OperationStats(key);
            }
            stats.recordExecution(metrics, success);
            return stats;
        });
    }

    /**
     * 리소스별 통계를 업데이트합니다.
     */
    private void updateResourceStats(LockOperationMetrics metrics, boolean success) {
        resourceStatsMap.compute(metrics.getResourceKey(), (key, stats) -> {
            if (stats == null) {
                stats = new ResourceStats(key);
            }
            stats.recordExecution(metrics, success);
            return stats;
        });
    }

    /**
     * 전체 성공률을 계산합니다.
     */
    private double calculateSuccessRate() {
        long total = totalOperations.get();
        return total > 0 ? (double) successfulOperations.get() / total * 100 : 0.0;
    }

    /**
     * 평균 재시도 횟수를 계산합니다.
     */
    private double calculateAverageRetries() {
        long total = totalOperations.get();
        return total > 0 ? (double) totalRetries.get() / total : 0.0;
    }

    /**
     * 평균 대기 시간을 계산합니다.
     */
    private double calculateAverageWaitTime() {
        long total = totalOperations.get();
        return total > 0 ? (double) totalWaitTime.get() / total : 0.0;
    }

    /**
     * 전체 통계 요약 정보를 담는 데이터 클래스입니다.
     */
    public static class StatisticsSummary {
        private long totalOperations;
        private long successfulOperations;
        private long failedOperations;
        private long totalRetries;
        private long totalWaitTime;
        private double successRate;
        private double averageRetries;
        private double averageWaitTime;
        private LocalDateTime generatedAt;

        // Getters and Setters
        public long getTotalOperations() { return totalOperations; }
        public void setTotalOperations(long totalOperations) { this.totalOperations = totalOperations; }
        
        public long getSuccessfulOperations() { return successfulOperations; }
        public void setSuccessfulOperations(long successfulOperations) { this.successfulOperations = successfulOperations; }
        
        public long getFailedOperations() { return failedOperations; }
        public void setFailedOperations(long failedOperations) { this.failedOperations = failedOperations; }
        
        public long getTotalRetries() { return totalRetries; }
        public void setTotalRetries(long totalRetries) { this.totalRetries = totalRetries; }
        
        public long getTotalWaitTime() { return totalWaitTime; }
        public void setTotalWaitTime(long totalWaitTime) { this.totalWaitTime = totalWaitTime; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageRetries() { return averageRetries; }
        public void setAverageRetries(double averageRetries) { this.averageRetries = averageRetries; }
        
        public double getAverageWaitTime() { return averageWaitTime; }
        public void setAverageWaitTime(double averageWaitTime) { this.averageWaitTime = averageWaitTime; }
        
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        @Override
        public String toString() {
            return String.format(
                "통계 요약 [생성시간: %s, 총 작업: %d, 성공: %d, 실패: %d, 성공률: %.2f%%, 평균 재시도: %.2f, 평균 대기: %.2fms]",
                generatedAt, totalOperations, successfulOperations, failedOperations, 
                successRate, averageRetries, averageWaitTime
            );
        }
    }
}