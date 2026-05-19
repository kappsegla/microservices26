package org.example.stockservice.repository;

import org.example.stockservice.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
