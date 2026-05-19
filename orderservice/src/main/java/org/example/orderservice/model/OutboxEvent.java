package org.example.orderservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private UUID eventId;
    private String aggregateType;
    private Long aggregateId;
    private String type;
    
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    public enum OutboxStatus {
        PENDING, PROCESSED, FAILED
    }

    public OutboxEvent() {}

    public OutboxEvent(UUID eventId, String aggregateType, Long aggregateId, String type, String payload) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
        this.status = OutboxStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getType() { return type; }
    public String getPayload() { return payload; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
}
