package com.homebudget.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
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

    @Autowired
    private DataSource dataSource;

    /**
     * Health check endpoint that verifies database connectivity.
     * Returns 200 OK when service and database are reachable.
     * Returns 503 Service Unavailable when database is unreachable.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "home-budget-backend");
        response.put("version", "1.0.0-SNAPSHOT");

        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2);
            response.put("status", "UP");
            response.put("database", "UP");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("database", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
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

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(
            @RequestHeader("X-Hass-User") String username) {
        Map<String, String> response = new HashMap<>();
        response.put("username", username);
        return ResponseEntity.ok(response);
    }
}
