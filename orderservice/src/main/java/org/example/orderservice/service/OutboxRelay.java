package org.example.orderservice.service;

import org.example.orderservice.config.RabbitConfig;
import org.example.event.OrderPlacedEvent;
import org.example.orderservice.model.OutboxEvent;
import org.example.orderservice.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxRelay {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final org.example.orderservice.controller.ChaosContext chaosContext;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate, org.example.orderservice.controller.ChaosContext chaosContext, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.chaosContext = chaosContext;
        this.objectMapper = objectMapper;
        
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack && correlationData != null) {
                Long id = Long.valueOf(correlationData.getId());
                updateStatus(id, OutboxEvent.OutboxStatus.PROCESSED);
                logger.info("Message {} successfully published and acked", id);
            } else if (correlationData != null) {
                Long id = Long.valueOf(correlationData.getId());
                logger.error("Message {} failed to publish: {}", id, cause);
                // Optionally retry or mark as FAILED
            }
        });
    }

    private void updateStatus(Long id, OutboxEvent.OutboxStatus status) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setStatus(status);
            outboxRepository.save(event);
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void relayEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING);
        for (OutboxEvent event : pendingEvents) {
            try {
                String payload = event.getPayload();
                var scenario = chaosContext.getCurrentScenario();
                
                if (scenario == org.example.orderservice.controller.ChaosScenario.DATA_CORRUPTION) {
                    payload = "{\"corrupted\": \"true\", \"quantity\": -99}";
                    logger.warn("Chaos: Corrupting payload for event {}", event.getEventId());
                }

                logger.info("Outbox Recovery: Relaying pending event {} (Aggregate ID: {})", event.getEventId(), event.getAggregateId());
                logger.debug("Relaying event: {} with scenario {}", event.getEventId(), scenario);
                CorrelationData correlationData = new CorrelationData(event.getId().toString());
                
                Object messagePayload = event.getPayload();
                if (scenario != org.example.orderservice.controller.ChaosScenario.DATA_CORRUPTION) {
                    try {
                        messagePayload = objectMapper.readValue(event.getPayload(), org.example.event.OrderPlacedEvent.class);
                    } catch (Exception e) {
                        logger.error("Failed to parse payload for event {}: {}", event.getEventId(), e.getMessage());
                    }
                } else {
                    payload = "{\"corrupted\": \"true\", \"quantity\": -99}";
                    messagePayload = payload;
                    logger.warn("Chaos: Corrupting payload for event {}", event.getEventId());
                }

                rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    "order.placed",
                    messagePayload,
                    message -> {
                        message.getMessageProperties().setHeader("X-Sender-App", "OrderService");
                        message.getMessageProperties().setHeader("X-Auth-Token", "d2lkZ2V0X3NlY3JldF90b2tlbg==");
                        return message;
                    },
                    correlationData
                );

                if (scenario == org.example.orderservice.controller.ChaosScenario.DUPLICATE_MESSAGE) {
                    logger.warn("Chaos: Sending duplicate message for event {}", event.getEventId());
                    rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE_NAME,
                        "order.placed",
                        messagePayload,
                        message -> {
                            message.getMessageProperties().setHeader("X-Sender-App", "OrderService");
                            message.getMessageProperties().setHeader("X-Auth-Token", "d2lkZ2V0X3NlY3JldF90b2tlbg==");
                            return message;
                        },
                        correlationData
                    );
                }
            } catch (Exception e) {
                logger.error("Error relaying event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
