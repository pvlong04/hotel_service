package org.example.hotel_service.services.auth;

import org.example.hotel_service.dtos.request.LoginRequest;
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.request.RefreshTokenRequest;
import org.example.hotel_service.dtos.response.AuthResponse;

public interface AuthenticationServiceImp {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String userAgent, String ipAddress);
    AuthResponse refreshToken(RefreshTokenRequest request, String userAgent, String ipAddress);
    void logout(RefreshTokenRequest request);
}
