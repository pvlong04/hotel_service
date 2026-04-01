package org.example.hotel_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.api.ApiResponse;
import org.hibernate.boot.model.naming.IllegalIdentifierException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandle {
    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse<?>> handingRuntimeException(RuntimeException exception) {
        log.error("Runtime error occurred: ", exception);
        ApiResponse<?> response = ApiResponse.builder()
                .code(ErrorCode.RUNTIME_ERROR.getCode())
                .message(ErrorCode.RUNTIME_ERROR.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handingException(Exception exception) {
        log.error("Unexpected error occurred: ", exception);
        ApiResponse<?> response = ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXIT.getCode())
                .message(ErrorCode.UNCATEGORIZED_EXIT.getMessage())
                .build();
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(value = SQLException.class)
    ResponseEntity<ApiResponse<?>> handingSQLException(SQLException exception) {
        log.error("Database error occurred: ", exception);
        ApiResponse<?> response = ApiResponse.builder()
                .code(ErrorCode.UNCATEGORIZED_EXIT.getCode())
                .message("Lỗi cơ sở dữ liệu")
                .build();
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(value = InvalidDataAccessApiUsageException.class)
    ResponseEntity<ApiResponse<?>> handingInvalidDataAccessApiUsageException(Exception exception) {
        ApiResponse<?> response = ApiResponse.builder()
                .code(ErrorCode.INVALID_DATA_ACCESS.getCode())
                .message(ErrorCode.INVALID_DATA_ACCESS.getMessage())
                .build();
        log.warn("Invalid data access: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    ResponseEntity<ApiResponse<?>> handingIllegalArgumentException(IllegalIdentifierException exception) {
        log.warn("Illegal argument: {}", exception.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCode.ILLEGAL_ARGUMENT.getCode())
                .message(ErrorCode.ILLEGAL_ARGUMENT.getMessage())
                .build();
        return ResponseEntity.badRequest().body(apiResponse);
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

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse<?>> handlingAccessDeniedException(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(ErrorCode.ACCESS_DENIED.getCode())
                .message(ErrorCode.ACCESS_DENIED.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse);
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


