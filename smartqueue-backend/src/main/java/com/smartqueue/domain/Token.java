package com.smartqueue.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_queue_id", nullable = false)
    private ServiceQueue serviceQueue;

    @Column(nullable = false, unique = true)
    private String tokenNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    private LocalDateTime expectedTime;

    @Column(nullable = false)
    private boolean alertSent = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime activatedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private boolean isVip = false;

    @Column(nullable = false)
    private LocalDateTime sortOrderTime;

    private LocalDateTime scheduledTime;
    private LocalDateTime scheduledEndTime;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sortOrderTime == null) {
            this.sortOrderTime = this.createdAt;
        }
    }

    public Token() {}

    public Token(User user, ServiceQueue serviceQueue, String tokenNumber, TokenStatus status) {
        this.user = user;
        this.serviceQueue = serviceQueue;
        this.tokenNumber = tokenNumber;
        this.status = status;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public ServiceQueue getServiceQueue() { return serviceQueue; }
    public void setServiceQueue(ServiceQueue serviceQueue) { this.serviceQueue = serviceQueue; }
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }
    public LocalDateTime getExpectedTime() { return expectedTime; }
    public void setExpectedTime(LocalDateTime expectedTime) { this.expectedTime = expectedTime; }
    public boolean isAlertSent() { return alertSent; }
    public void setAlertSent(boolean alertSent) { this.alertSent = alertSent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public boolean isVip() { return isVip; }
    public void setVip(boolean vip) { isVip = vip; }
    public LocalDateTime getSortOrderTime() { return sortOrderTime; }
    public void setSortOrderTime(LocalDateTime sortOrderTime) { this.sortOrderTime = sortOrderTime; }
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    public LocalDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(LocalDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }
}
