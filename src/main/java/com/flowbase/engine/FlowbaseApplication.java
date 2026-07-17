package com.flowbase.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlowbaseApplication {
    
    public static void main(String[] args) {
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                java.nio.file.Files.readAllLines(envPath).forEach(line -> {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("#") && trimmed.contains("=")) {
                        int splitIdx = trimmed.indexOf("=");
                        String key = trimmed.substring(0, splitIdx).trim();
                        String value = trimmed.substring(splitIdx + 1).trim();
                        System.setProperty(key, value); // Load into JVM System environment context
                    }
                });
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
        }
        SpringApplication.run(FlowbaseApplication.class, args);
    }
    
}
