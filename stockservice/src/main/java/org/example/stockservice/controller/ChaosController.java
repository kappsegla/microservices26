package org.example.stockservice.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chaos")
public class ChaosController {
    private final ChaosContext chaosContext;

    public ChaosController(ChaosContext chaosContext) {
        this.chaosContext = chaosContext;
    }

    @PostMapping("/scenario")
    public void setScenario(@RequestBody ChaosScenario scenario) {
        chaosContext.setCurrentScenario(scenario);
    }

    @GetMapping("/scenario")
    public ChaosScenario getScenario() {
        return chaosContext.getCurrentScenario();
    }
}
