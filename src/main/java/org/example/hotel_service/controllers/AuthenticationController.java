package org.example.hotel_service.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.LoginRequest;
import org.example.hotel_service.dtos.request.RefreshTokenRequest;
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.services.auth.AuthenticationService;
import org.example.hotel_service.services.auth.AuthenticationServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationController {
    AuthenticationServiceImp authService;

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
//        return ApiResponse.success(authService.register(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AuthResponse>builder()
                .data(authService.register(request))
                .build());
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<AuthResponse>builder()
                .data(authService.login(request, httpServletRequest.getHeader("User-Agent"), httpServletRequest.getRemoteAddr()))
                .build());
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<AuthResponse>builder()
                .data(authService.refreshToken(request, httpServletRequest.getHeader("User-Agent"), httpServletRequest.getRemoteAddr()))
                .build());
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<Void>builder().build());
    }
}
