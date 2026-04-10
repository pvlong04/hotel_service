package org.example.hotel_service.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

/**
 * DTO phản hồi xác thực (login/register)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String tokenType;
    Long expiresIn; // seconds
    UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserInfo {
        UUID userId;
        String username;
        String email;
        String fullName;
        String avatarUrl;
        Set<String> roles;
    }
}
