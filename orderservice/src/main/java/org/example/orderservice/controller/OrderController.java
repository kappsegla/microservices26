package org.example.orderservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.orderservice.model.Order;
import org.example.orderservice.service.OrderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final ChaosContext chaosContext;

    public OrderController(OrderService orderService, ChaosContext chaosContext) {
        this.orderService = orderService;
        this.chaosContext = chaosContext;
    }

    @PostMapping
    public Order placeOrder(@RequestBody Order order) throws JsonProcessingException {
        Order savedOrder = orderService.placeOrder(order);
        
        if (chaosContext.getCurrentScenario() == ChaosScenario.FAIL_BEFORE_PUBLISH) {
            throw new RuntimeException("Chaos: Failing before publish (simulating crash after save)");
        }
        
        return savedOrder;
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}
