package org.example.hotel_service.AI.service;

import org.example.hotel_service.AI.dtos.response.AIResponse;

/**
 * Interface for AI Chat Service
 */
public interface AIChatServiceImp {
    AIResponse chat(String message, String sessionId);
}
