package org.example.hotel_service.dtos.response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi lỗi validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private boolean success;
    private String message;
    private Map<String, String> errors; // field -> error message
    private Long timestamp;

    public static ValidationErrorResponse of(Map<String, String> errors) {
        return ValidationErrorResponse.builder()
                .success(false)
                .message("Validation failed")
                .errors(errors)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
