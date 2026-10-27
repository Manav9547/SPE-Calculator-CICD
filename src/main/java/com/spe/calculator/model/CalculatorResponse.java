package com.spe.calculator.model;

public record CalculatorResponse(
        String operation,
        double a,
        double b,
        double result
) {}
