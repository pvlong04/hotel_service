package org.example.hotel_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandle {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handingException(Exception exception) {
        log.error("Unexpected error occurred: ", exception);
        ApiResponse<?> response = ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXIT.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXIT.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(value = ApiException.class)
    ResponseEntity<ApiResponse<?>> handlingApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("API Exception: {} - {}", errorCode.getCode(), errorCode.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        log.warn("Validation failed: {}", errors);
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCode.KEY_VALID.getCode())
                .message("Dữ liệu không hợp lệ")
                .data(errors)
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
    }

}
