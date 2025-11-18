package com.cognify.cognify_backend.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Load environment variables from .env file before Spring Boot starts
 * This allows application.properties to use ${ENV_VAR} syntax
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> envMap = new HashMap<>();

            // Load all dotenv entries into Spring environment
            dotenv.entries().forEach(entry -> {
                envMap.put(entry.getKey(), entry.getValue());
                // Also set as system property for compatibility
                System.setProperty(entry.getKey(), entry.getValue());
            });

            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", envMap));

            System.out.println("✓ Loaded .env file with " + envMap.size() + " environment variables");
        } catch (Exception e) {
            System.err.println("⚠ Could not load .env file: " + e.getMessage());
        }
    }
}
