package org.example.hotel_service.exception;

import lombok.Value;
import org.example.hotel_service.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandle {
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handingException(Exception exception) {
        ApiResponse response = new ApiResponse<>();
        response.setCode(ErrorCode.UNCATEGORIZED_EXIT.getCode());
        response.setMessage(ErrorCode.UNCATEGORIZED_EXIT.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(value = ApiException.class)
    ResponseEntity<ApiResponse> handlingApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse<>();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }


}
