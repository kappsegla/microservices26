package org.example.stockservice.service;

import org.example.stockservice.config.RabbitConfig;
import org.example.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockListener {
    private static final Logger logger = LoggerFactory.getLogger(StockListener.class);
    private final StockService stockService;

    public StockListener(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleOrderPlaced(
            OrderPlacedEvent event,
            @org.springframework.messaging.handler.annotation.Header(required = false, name = "x-delivery-attempt") Integer deliveryAttempt,
            @org.springframework.messaging.handler.annotation.Header(required = false, name = "X-Sender-App") String senderApp,
            @org.springframework.messaging.handler.annotation.Header(required = false, name = "X-Auth-Token") String authToken) {
        
        if (deliveryAttempt != null && deliveryAttempt > 1) {
            logger.info("Retry detected! Delivery attempt: {}", deliveryAttempt);
        }
        
        // Sender verification / authentication simulation
        if (authToken != null && !authToken.equals("d2lkZ2V0X3NlY3JldF90b2tlbg==")) {
            logger.warn("Authentication failed! Invalid token received from {}: {}", senderApp, authToken);
            throw new SecurityException("Invalid authentication token");
        } else if (senderApp != null) {
            logger.info("Authentication successful! Message received from trusted source: {} (Auth Token: {})", senderApp, authToken);
        } else {
            logger.warn("Unauthenticated message! Missing sender application headers.");
        }
        
        // Modern Java Feature: Record Pattern in switch
        switch (event) {
            case OrderPlacedEvent(var eventId, var orderId, var product, var quantity, var price) -> {
                logger.info("Received order event via Record Pattern: id={}, product={}, qty={}", eventId, product, quantity);
                stockService.processOrder(event);
            }
        }
    }
}
