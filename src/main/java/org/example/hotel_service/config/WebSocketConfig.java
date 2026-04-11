package org.example.hotel_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:3000,http://localhost:5173,https://*.ngrok-free.dev,https://*.onrender.com,https://*.vercel.app,https://*.netlify.app}")
    private String allowedOriginPatterns;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Server → Client prefixes
        config.enableSimpleBroker("/topic", "/queue");
        // Client → Server prefix
        config.setApplicationDestinationPrefixes("/app");
        // User-specific destination prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(parseOriginPatterns())
                .withSockJS();
    }

    private String[] parseOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
