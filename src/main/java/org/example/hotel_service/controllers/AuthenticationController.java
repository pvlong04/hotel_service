package org.example.hotel_service.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.LoginRequest;
import org.example.hotel_service.dtos.request.RefreshTokenRequest;
import org.example.hotel_service.dtos.request.ResendVerificationRequest;
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.services.auth.AuthenticationServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationServiceImp authService;

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký tài khoản thành công", authService.register(request)));
    }

    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Đăng nhập thành công",
                        authService.login(request, httpServletRequest.getHeader("User-Agent"), httpServletRequest.getRemoteAddr())));
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody @Valid RefreshTokenRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Làm mới token thành công",
                        authService.refreshToken(request, httpServletRequest.getHeader("User-Agent"), httpServletRequest.getRemoteAddr())));
    }

    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Đăng xuất thành công", null));
    }

    @GetMapping("/verify-email")
    ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Xac thuc email thanh cong", null));
    }

    @PostMapping("/resend-verification")
    ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody @Valid ResendVerificationRequest request) {
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.success("Neu email ton tai, he thong da gui lai link xac thuc", null));
    }
}
