package org.example.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE_NAME = "order.exchange";
    public static final String STOCK_FAILED_QUEUE = "stock.failed.queue";
    public static final String STOCK_FAILED_EXCHANGE = "stock.failed.exchange";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue stockFailedQueue() {
        return new Queue(STOCK_FAILED_QUEUE);
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
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
