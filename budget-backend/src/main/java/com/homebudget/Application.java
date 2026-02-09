package com.homebudget;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for Home Budget Tracker backend.
 *
 * This application provides REST APIs for budget tracking functionality
 * and integrates with Home Assistant authentication via X-Hass-User header.
 */
@SpringBootApplication
@EnableScheduling
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.default-dev-user:}")
    private String defaultDevUser;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void logStartupMode() {
        if (devMode) {
            logger.warn("APPLICATION RUNNING IN DEVELOPMENT MODE - Default user authentication enabled");
            logger.warn("X-Hass-User header will default to '{}' when not provided", defaultDevUser);
        } else {
            logger.info("APPLICATION RUNNING IN PRODUCTION MODE - X-Hass-User header required");
        }
    }
}
