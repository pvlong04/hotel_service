package org.example.hotel_service.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO phản hồi thông tin người dùng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    UUID userId;
    String username;
    String email;
    String status;
    LocalDateTime lastLoginAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Profile
    String fullName;
    String phone;
    String avatarUrl;
    String address;
    LocalDate dob;
    String gender;
    String nationalId;
    String nationality;

    // Roles
    Set<String> roles;
}
