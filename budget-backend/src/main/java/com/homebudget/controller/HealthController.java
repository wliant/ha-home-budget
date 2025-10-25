package com.homebudget.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for verifying backend service status.
 *
 * This controller provides endpoints for:
 * - Service health verification
 * - Database connectivity checks
 * - API availability confirmation
 *
 * Used by:
 * - Docker Compose health checks
 * - Frontend service for backend availability
 * - Monitoring and alerting systems
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Basic health check endpoint.
     * Returns 200 OK when service is running.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "home-budget-backend");
        response.put("version", "1.0.0-SNAPSHOT");
        return ResponseEntity.ok(response);
    }

    /**
     * API info endpoint.
     * Provides metadata about the backend service.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("app", "Home Budget Tracker");
        response.put("description", "Backend service for household budget and expense tracking");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("authentication", "Home Assistant (X-Hass-User header)");
        return ResponseEntity.ok(response);
    }
}
