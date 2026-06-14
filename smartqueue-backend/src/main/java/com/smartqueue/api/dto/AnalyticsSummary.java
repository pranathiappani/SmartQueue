package com.smartqueue.api.dto;

import java.util.List;

public class AnalyticsSummary {
    private long totalQueues;
    private long totalTokensWaiting;
    private long totalTokensCompleted;
    private List<QueueStat> queueStats;

    public long getTotalQueues() { return totalQueues; }
    public void setTotalQueues(long totalQueues) { this.totalQueues = totalQueues; }

    public long getTotalTokensWaiting() { return totalTokensWaiting; }
    public void setTotalTokensWaiting(long totalTokensWaiting) { this.totalTokensWaiting = totalTokensWaiting; }

    public long getTotalTokensCompleted() { return totalTokensCompleted; }
    public void setTotalTokensCompleted(long totalTokensCompleted) { this.totalTokensCompleted = totalTokensCompleted; }

    public List<QueueStat> getQueueStats() { return queueStats; }
    public void setQueueStats(List<QueueStat> queueStats) { this.queueStats = queueStats; }
}
