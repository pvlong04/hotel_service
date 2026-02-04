package org.example.hotel_service.enums;

/**
 * Mục đích của auth token
 */
public enum TokenPurpose {
    VERIFY_EMAIL,       // Xác thực email qua link
    VERIFY_EMAIL_OTP,   // Xác thực email qua OTP
    RESET_PASSWORD      // Đặt lại mật khẩu
}
