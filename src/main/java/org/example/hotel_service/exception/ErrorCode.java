package org.example.hotel_service.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNCATEGORIZED_EXIT(9999, "Lỗi hệ thống"),
    USER_EXIT_EMAIL(1001, "Email người dùng đã tồn tại"),
    USER_NOT_FOUND(1002, "Không tìm thấy người dùng"),
    USERNAME_VALID(1003, "Tên người dùng ít nhất là 3 ký tự"),
    PASSWORD_VALID(1004, "Mật khẩu ít nhất là 8 ký tự"),
    KEY_VALID(1005, "Lỗi hệ thống"),
    USER_NOT_EXIT(1006, "Người dùng không tồn tại"),
    UNAUTHENTICATED(1007, "Đăng nhập thất bại"),
    USER_EXITS(1008, "Người dùng đã tồn tại"),
    INVALID_REFRESH_TOKEN(1009, "Refresh token không hợp lệ hoặc đã bị thu hồi"),
    REFRESH_TOKEN_EXPIRED(1010, "Refresh token đã hết hạn");

    int code;
    String message;

}
