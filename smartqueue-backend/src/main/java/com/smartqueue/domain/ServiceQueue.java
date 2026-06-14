package com.smartqueue.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "service_queues")
public class ServiceQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer averageServiceTimeMinutes = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueType type = QueueType.LIVE;

    private Integer slotDurationMinutes;
    private Integer slotBufferMinutes;
    private LocalTime operatingHoursStart;
    private LocalTime operatingHoursEnd;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ServiceQueue() {}

    public ServiceQueue(String name, String description, Integer averageServiceTimeMinutes) {
        this.name = name;
        this.description = description;
        this.averageServiceTimeMinutes = averageServiceTimeMinutes;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getAverageServiceTimeMinutes() { return averageServiceTimeMinutes; }
    public void setAverageServiceTimeMinutes(Integer averageServiceTimeMinutes) { this.averageServiceTimeMinutes = averageServiceTimeMinutes; }
    public QueueType getType() { return type; }
    public void setType(QueueType type) { this.type = type; }
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) { this.slotDurationMinutes = slotDurationMinutes; }
    public Integer getSlotBufferMinutes() { return slotBufferMinutes; }
    public void setSlotBufferMinutes(Integer slotBufferMinutes) { this.slotBufferMinutes = slotBufferMinutes; }
    public LocalTime getOperatingHoursStart() { return operatingHoursStart; }
    public void setOperatingHoursStart(LocalTime operatingHoursStart) { this.operatingHoursStart = operatingHoursStart; }
    public LocalTime getOperatingHoursEnd() { return operatingHoursEnd; }
    public void setOperatingHoursEnd(LocalTime operatingHoursEnd) { this.operatingHoursEnd = operatingHoursEnd; }
    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
