package com.spe.calculator.controller;

import com.spe.calculator.model.CalculatorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

    @GetMapping("/add")
    public CalculatorResponse add(@RequestParam double a, @RequestParam double b) {
        return response("add", a, b, a + b);
    }

    @GetMapping("/subtract")
    public CalculatorResponse subtract(@RequestParam double a, @RequestParam double b) {
        return response("subtract", a, b, a - b);
    }

    @GetMapping("/multiply")
    public CalculatorResponse multiply(@RequestParam double a, @RequestParam double b) {
        return response("multiply", a, b, a * b);
    }

    @GetMapping("/divide")
    public CalculatorResponse divide(@RequestParam double a, @RequestParam double b) {
        if (b == 0.0) {
            throw new IllegalArgumentException("Division by zero is not allowed");
        }
        return response("divide", a, b, a / b);
    }

    private CalculatorResponse response(String operation, double a, double b, double result) {
        return new CalculatorResponse(operation, a, b, result);
    }
}
