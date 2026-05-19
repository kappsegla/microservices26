package org.example.stockservice.controller;

import org.example.stockservice.model.Stock;
import org.example.stockservice.repository.StockRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/stocks")
public class StockController {
    private final StockRepository stockRepository;

    public StockController(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @GetMapping
    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }
}
