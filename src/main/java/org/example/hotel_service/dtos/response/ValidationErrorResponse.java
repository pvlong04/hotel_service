package org.example.hotel_service.dtos.response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO phản hồi lỗi validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationErrorResponse {
    boolean success;
    String message;
    Map<String, String> errors; // field -> error message
    Long timestamp;

    public static ValidationErrorResponse of(Map<String, String> errors) {
        return ValidationErrorResponse.builder()
                .success(false)
                .message("Validation failed")
                .errors(errors)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
