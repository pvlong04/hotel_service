package org.example.hotel_service.AI.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatRequest {
    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2000, message = "Message is too long")
    private String message;

    @Size(max = 100, message = "Session ID is too long")
    private String sessionId;
}
