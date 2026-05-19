package org.example.event;

import java.util.UUID;

public record StockReservedEvent(
    UUID eventId,
    Long orderId
) {}
