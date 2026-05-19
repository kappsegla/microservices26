package org.example.orderservice.controller;

import org.springframework.stereotype.Component;

@Component
public class ChaosContext {
    private ChaosScenario currentScenario = ChaosScenario.NORMAL;

    public ChaosScenario getCurrentScenario() {
        return currentScenario;
    }

    public void setCurrentScenario(ChaosScenario currentScenario) {
        this.currentScenario = currentScenario;
    }
}
