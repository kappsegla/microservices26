package org.example.stockservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String QUEUE_NAME = "stock.orders.queue";
    public static final String DLQ_NAME = "stock.orders.dlq";
    public static final String EXCHANGE_NAME = "order.exchange";
    public static final String DLX_NAME = "order.dlx";
    public static final String STOCK_FAILED_EXCHANGE = "stock.failed.exchange";
    public static final String STOCK_FAILED_QUEUE = "stock.failed.queue";

    @Bean
    public Queue stockOrdersQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", "stock.orders.dlq")
            .build();
    }

    @Bean
    public Queue stockFailedQueue() {
        return QueueBuilder.durable(STOCK_FAILED_QUEUE).build();
    }

    @Bean
    public TopicExchange stockFailedExchange() {
        return new TopicExchange(STOCK_FAILED_EXCHANGE);
    }

    @Bean
    public Binding stockFailedBinding(Queue stockFailedQueue, TopicExchange stockFailedExchange) {
        return BindingBuilder.bind(stockFailedQueue).to(stockFailedExchange).with("stock.reservation.failed");
    }

    @Bean
    public Binding stockReservedBinding(Queue stockFailedQueue, TopicExchange stockFailedExchange) {
        return BindingBuilder.bind(stockFailedQueue).to(stockFailedExchange).with("stock.reserved");
    }

    @Bean
    public Queue stockOrdersDlq() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange orderDlx() {
        return new TopicExchange(DLX_NAME);
    }

    @Bean
    public Binding binding(Queue stockOrdersQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(stockOrdersQueue).to(orderExchange).with("order.placed");
    }

    @Bean
    public Binding dlqBinding(Queue stockOrdersDlq, TopicExchange orderDlx) {
        return BindingBuilder.bind(stockOrdersDlq).to(orderDlx).with("stock.orders.dlq");
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
