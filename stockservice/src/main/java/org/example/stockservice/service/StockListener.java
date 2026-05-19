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
    public void handleOrderPlaced(OrderPlacedEvent event, @org.springframework.messaging.handler.annotation.Header(required = false, name = "x-delivery-attempt") Integer deliveryAttempt) {
        if (deliveryAttempt != null && deliveryAttempt > 1) {
            logger.info("Retry detected! Delivery attempt: {}", deliveryAttempt);
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
