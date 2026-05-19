package org.example.event;

import java.util.UUID;

public record OrderPlacedEvent(
    UUID eventId,
    Long orderId,
    String product,
    Integer quantity,
    java.math.BigDecimal price
) {}
