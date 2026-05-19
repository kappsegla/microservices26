package org.example.event;

import java.util.UUID;

public record StockReservationFailedEvent(
    UUID eventId,
    Long orderId,
    String reason
) {}
