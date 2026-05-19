package org.example.stockservice.controller;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChaosContext {
    private ChaosScenario currentScenario = ChaosScenario.NORMAL;
    private final Map<UUID, Integer> attemptCounts = new ConcurrentHashMap<>();

    public ChaosScenario getCurrentScenario() {
        return currentScenario;
    }

    public void setCurrentScenario(ChaosScenario currentScenario) {
        this.currentScenario = currentScenario;
        this.attemptCounts.clear();
    }

    public int incrementAndGetAttempt(UUID eventId) {
        return attemptCounts.merge(eventId, 1, Integer::sum);
    }
}
