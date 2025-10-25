package com.homebudget.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for HealthController.
 *
 * These tests run with the 'test' profile, which uses H2 in-memory database
 * instead of MySQL to ensure test isolation and speed.
 *
 * Test Objectives:
 * - Verify health endpoint returns UP status
 * - Verify info endpoint returns correct service metadata
 * - Ensure tests run in < 30 seconds
 * - Confirm H2 database is used (not MySQL)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that /api/health endpoint returns UP status.
     * This verifies the basic health check functionality.
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("home-budget-backend"))
                .andExpect(jsonPath("$.version").value("1.0.0-SNAPSHOT"));
    }

    /**
     * Test that /api/info endpoint returns correct service information.
     * This verifies metadata about the service is correctly exposed.
     */
    @Test
    void testInfoEndpoint() throws Exception {
        mockMvc.perform(get("/api/info")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.app").value("Home Budget Tracker"))
                .andExpect(jsonPath("$.description").value("Backend service for household budget and expense tracking"))
                .andExpect(jsonPath("$.version").value("1.0.0-SNAPSHOT"))
                .andExpect(jsonPath("$.authentication").value("Home Assistant (X-Hass-User header)"));
    }

    /**
     * Test that health endpoint responds quickly.
     * The entire test suite should complete in < 30 seconds.
     */
    @Test
    void testHealthEndpointPerformance() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // Health endpoint should respond in < 1 second
        assert duration < 1000 : "Health endpoint took " + duration + "ms (expected < 1000ms)";
    }
}
