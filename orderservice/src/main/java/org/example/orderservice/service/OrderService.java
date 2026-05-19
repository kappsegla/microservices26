package org.example.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.event.OrderPlacedEvent;
import org.example.orderservice.model.Order;
import org.example.orderservice.model.OutboxEvent;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order placeOrder(Order order) throws JsonProcessingException {
        Order savedOrder = orderRepository.save(order);
        
        OrderPlacedEvent event = new OrderPlacedEvent(
            UUID.randomUUID(),
            savedOrder.getId(),
            savedOrder.getProduct(),
            savedOrder.getQuantity(),
            savedOrder.getPrice()
        );
        
        String payload = objectMapper.writeValueAsString(event);
        
        OutboxEvent outboxEvent = new OutboxEvent(
            event.eventId(),
            "ORDER",
            savedOrder.getId(),
            "ORDER_PLACED",
            payload
        );
        
        outboxRepository.save(outboxEvent);
        
        return savedOrder;
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
