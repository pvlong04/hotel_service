package org.example.hotel_service.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig {
    // CORS is configured centrally in SecurityConfig#corsConfigurationSource.
    // Keep this class to avoid breaking package scan references.
}
