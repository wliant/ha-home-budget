package com.homebudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for Home Budget Tracker backend.
 *
 * This application provides REST APIs for budget tracking functionality
 * and integrates with Home Assistant authentication via X-Hass-User header.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
