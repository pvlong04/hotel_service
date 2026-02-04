package org.example.hotel_service.enums;

/**
 * Trạng thái tài khoản người dùng
 */
public enum UserStatus {
    PENDING,    // Chờ xác thực email
    ACTIVE,     // Đã kích hoạt
    BANNED      // Bị khóa
}
