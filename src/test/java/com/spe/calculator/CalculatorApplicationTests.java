package com.spe.calculator;

import com.spe.calculator.controller.CalculatorController;
import com.spe.calculator.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CalculatorController.class)
@Import(GlobalExceptionHandler.class)
class CalculatorApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAddition() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "10")
                        .param("b", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("add"))
                .andExpect(jsonPath("$.result").value(15.0));
    }

    @Test
    void testSubtraction() throws Exception {
        mockMvc.perform(get("/api/v1/subtract")
                        .param("a", "10")
                        .param("b", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("subtract"))
                .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void testMultiplication() throws Exception {
        mockMvc.perform(get("/api/v1/multiply")
                        .param("a", "10")
                        .param("b", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("multiply"))
                .andExpect(jsonPath("$.result").value(50.0));
    }

    @Test
    void testDivision() throws Exception {
        mockMvc.perform(get("/api/v1/divide")
                        .param("a", "10")
                        .param("b", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("divide"))
                .andExpect(jsonPath("$.result").value(2.0));
    }

    @Test
    void testDivisionByZero() throws Exception {
        mockMvc.perform(get("/api/v1/divide")
                        .param("a", "10")
                        .param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Division by zero is not allowed"))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"));
    }

    @Test
    void testMissingParameter() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Missing or invalid request parameter"));
    }

    @Test
    void testInvalidParameterType() throws Exception {
        mockMvc.perform(get("/api/v1/add")
                        .param("a", "hello")
                        .param("b", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Missing or invalid request parameter"));
    }

    @Test
    void testErrorResponseContainsTimestampAndPath() throws Exception {
        mockMvc.perform(get("/api/v1/divide")
                        .param("a", "10")
                        .param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"));
    }
}
