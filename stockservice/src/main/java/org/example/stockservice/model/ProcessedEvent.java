package org.example.stockservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private UUID eventId;
    private LocalDateTime processedAt;

    public ProcessedEvent() {}

    public ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }

    public UUID getEventId() { return eventId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
