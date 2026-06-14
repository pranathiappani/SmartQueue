package com.smartqueue.api.dto;

public class QueueStat {
    private String queueName;
    private long totalTokens;
    private int avgWaitTime;

    public String getQueueName() { return queueName; }
    public void setQueueName(String queueName) { this.queueName = queueName; }

    public long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }

    public int getAvgWaitTime() { return avgWaitTime; }
    public void setAvgWaitTime(int avgWaitTime) { this.avgWaitTime = avgWaitTime; }
}
