package org.example.stockservice.config;

import org.example.stockservice.model.Stock;
import org.example.stockservice.repository.StockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(StockRepository stockRepository) {
        return args -> {
            if (stockRepository.count() == 0) {
                stockRepository.save(new Stock("Widget", 100));
                stockRepository.save(new Stock("ExpensiveItem", 100));
            }
        };
    }
}
