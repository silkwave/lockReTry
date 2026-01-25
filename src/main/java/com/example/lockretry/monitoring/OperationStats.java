package com.example.lockretry.monitoring;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 작업 유형별 통계 정보를 저장하는 데이터 클래스입니다.
 */
public class OperationStats {
    private final String operationType;
    private final AtomicInteger totalExecutions = new AtomicInteger(0);
    private final AtomicInteger successfulExecutions = new AtomicInteger(0);
    private final AtomicInteger failedExecutions = new AtomicInteger(0);
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    private final AtomicLong minExecutionTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTime = new AtomicLong(0);

    public OperationStats(String operationType) {
        this.operationType = operationType;
    }

    public void recordExecution(LockOperationMetrics metrics, boolean success) {
        totalExecutions.incrementAndGet();
        
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }
        
        totalRetries.addAndGet(metrics.getRetryCount().get());
        totalWaitTime.addAndGet(metrics.getTotalWaitTimeMs().get());
        
        long executionTime = metrics.getTotalExecutionTimeMs();
        updateMinExecutionTime(executionTime);
        updateMaxExecutionTime(executionTime);
    }

    private void updateMinExecutionTime(long time) {
        long current;
        do {
            current = minExecutionTime.get();
        } while (time < current && !minExecutionTime.compareAndSet(current, time));
    }

    private void updateMaxExecutionTime(long time) {
        long current;
        do {
            current = maxExecutionTime.get();
        } while (time > current && !maxExecutionTime.compareAndSet(current, time));
    }

    public String getOperationType() { return operationType; }
    public int getTotalExecutions() { return totalExecutions.get(); }
    public int getSuccessfulExecutions() { return successfulExecutions.get(); }
    public int getFailedExecutions() { return failedExecutions.get(); }
    public long getTotalRetries() { return totalRetries.get(); }
    public long getTotalWaitTime() { return totalWaitTime.get(); }
    public long getMinExecutionTime() { 
        long min = minExecutionTime.get();
        return min == Long.MAX_VALUE ? 0 : min; 
    }
    public long getMaxExecutionTime() { return maxExecutionTime.get(); }

    public double getSuccessRate() {
        int total = totalExecutions.get();
        return total > 0 ? (double) successfulExecutions.get() / total * 100 : 0.0;
    }

    public double getAverageRetries() {
        int total = totalExecutions.get();
        return total > 0 ? (double) totalRetries.get() / total : 0.0;
    }

    public double getAverageWaitTime() {
        int total = totalExecutions.get();
        return total > 0 ? (double) totalWaitTime.get() / total : 0.0;
    }

    @Override
    public String toString() {
        return String.format(
            "작업 통계 [유형: %s, 실행: %d, 성공: %d, 실패: %d, 성공률: %.2f%%, 평균 재시도: %.2f, 평균 대기: %.2fms, 최소/최대 시간: %d/%dms]",
            operationType, getTotalExecutions(), getSuccessfulExecutions(), getFailedExecutions(),
            getSuccessRate(), getAverageRetries(), getAverageWaitTime(), getMinExecutionTime(), getMaxExecutionTime()
        );
    }
}