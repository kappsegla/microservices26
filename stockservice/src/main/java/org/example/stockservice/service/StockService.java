package org.example.stockservice.service;

import org.example.stockservice.controller.ChaosContext;
import org.example.stockservice.controller.ChaosScenario;
import org.example.event.OrderPlacedEvent;
import org.example.event.StockReservationFailedEvent;
import org.example.event.StockReservedEvent;
import org.example.stockservice.model.ProcessedEvent;
import org.example.stockservice.model.Stock;
import org.example.stockservice.repository.ProcessedEventRepository;
import org.example.stockservice.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);
    private final StockRepository stockRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ChaosContext chaosContext;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    public StockService(StockRepository stockRepository, ProcessedEventRepository processedEventRepository, ChaosContext chaosContext, org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        this.stockRepository = stockRepository;
        this.processedEventRepository = processedEventRepository;
        this.chaosContext = chaosContext;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void processOrder(OrderPlacedEvent event) {
        logger.info("Processing event: {}", event);

        // 1. Check Idempotency
        if (processedEventRepository.existsById(event.eventId())) {
            logger.info("Event {} already processed. Skipping to ensure idempotency.", event.eventId());
            return;
        }

        // 2. Handle Chaos Scenarios
        handleChaos(event);

        // 3. Business Logic
        Stock stock = stockRepository.findById(event.product())
                .orElse(new Stock(event.product(), 100)); // Default 100 if not exists
        
        if (event.quantity() < 0) {
            throw new IllegalArgumentException("Chaos: Negative quantity detected for event " + event.eventId());
        }

        if (stock.getQuantity() < event.quantity()) {
            logger.warn("Insufficient stock for product {}. Required: {}, Available: {}", event.product(), event.quantity(), stock.getQuantity());
            
            // Compensating Action: Publish Failure Event
            StockReservationFailedEvent failedEvent = new StockReservationFailedEvent(
                java.util.UUID.randomUUID(),
                event.orderId(),
                "OUT_OF_STOCK"
            );
            
            rabbitTemplate.convertAndSend(
                org.example.stockservice.config.RabbitConfig.STOCK_FAILED_EXCHANGE,
                "stock.reservation.failed",
                failedEvent
            );
            
            // Still mark the event as processed to avoid retrying a known failure
            processedEventRepository.save(new org.example.stockservice.model.ProcessedEvent(event.eventId()));
            return;
        }

        stock.setQuantity(stock.getQuantity() - event.quantity());
        stockRepository.save(stock);

        // Success: Publish StockReservedEvent
        StockReservedEvent reservedEvent = new StockReservedEvent(
            java.util.UUID.randomUUID(),
            event.orderId()
        );

        rabbitTemplate.convertAndSend(
            org.example.stockservice.config.RabbitConfig.STOCK_FAILED_EXCHANGE,
            "stock.reserved",
            reservedEvent
        );

        // 4. Mark as processed
        processedEventRepository.save(new org.example.stockservice.model.ProcessedEvent(event.eventId()));
        logger.info("Successfully processed event {}", event.eventId());
    }

    private void handleChaos(OrderPlacedEvent event) {
        ChaosScenario scenario = chaosContext.getCurrentScenario();
        
        if (scenario == ChaosScenario.TRANSIENT_FAILURE) {
            int attempt = chaosContext.incrementAndGetAttempt(event.eventId());
            if (attempt <= 2) {
                logger.warn("Chaos: Simulating transient failure (attempt {}) for event {}", attempt, event.eventId());
                throw new RuntimeException("Chaos: Transient failure");
            }
        }
    }
}
