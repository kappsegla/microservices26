package org.example.orderservice.service;

import org.example.event.StockReservationFailedEvent;
import org.example.event.StockReservedEvent;
import org.example.orderservice.model.Order;
import org.example.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RabbitListener(queues = "stock.failed.queue")
public class OrderSagaListener {
    private static final Logger logger = LoggerFactory.getLogger(OrderSagaListener.class);
    private final OrderRepository orderRepository;

    public OrderSagaListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @RabbitHandler
    @Transactional
    public void handleStockReservationFailed(StockReservationFailedEvent event) {
        logger.warn("Received stock reservation failure for order {}. Reason: {}", event.orderId(), event.reason());
        
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            order.setStatus(Order.OrderStatus.CANCELLED_OUT_OF_STOCK);
            orderRepository.save(order);
            logger.info("Order {} status updated to CANCELLED_OUT_OF_STOCK", event.orderId());
        }, () -> {
            logger.error("Order {} not found while processing compensating action", event.orderId());
        });
    }

    @RabbitHandler
    @Transactional
    public void handleStockReserved(StockReservedEvent event) {
        logger.info("Received stock reservation confirmation for order {}", event.orderId());
        
        orderRepository.findById(event.orderId()).ifPresentOrElse(order -> {
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);
            logger.info("Order {} status updated to COMPLETED", event.orderId());
        }, () -> {
            logger.error("Order {} not found while processing confirmation", event.orderId());
        });
    }

    @RabbitHandler(isDefault = true)
    public void handleUnknown(Object object) {
        logger.warn("Received unknown message type: {}", object.getClass().getName());
    }
}
