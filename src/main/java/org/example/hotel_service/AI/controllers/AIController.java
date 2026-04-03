package org.example.hotel_service.AI.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.AI.dtos.request.AIChatRequest;
import org.example.hotel_service.AI.dtos.response.AIResponse;
import org.example.hotel_service.AI.service.AIChatServiceImp;
import org.example.hotel_service.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIController {

    AIChatServiceImp aiChatService;

    @PostMapping
    public ResponseEntity<ApiResponse<AIResponse>> chat(@Valid @RequestBody AIChatRequest request) {
        AIResponse result = aiChatService.chat(request.getMessage(), request.getSessionId());
        return ResponseEntity.ok(ApiResponse.success("AI trả lời thành công", result));
    }
}
