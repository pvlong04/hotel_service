package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.UserStatus;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;

/**
 * DTO yêu cầu cập nhật người dùng (ADMIN: full; STAFF/GUEST: chỉ profile)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {

    @Size(min  = 8, max = 120, message = "Họ tên không quá 120 ký tự")
    String fullName;

    @Size(min = 9 ,max = 15, message = "Số điện thoại không quá 30 ký tự")
    String phone;

    @Size(min = 100, max = 500, message = "URL avatar không quá 500 ký tự")
    String avatarUrl;

    @Size(min  = 8, max = 255, message = "Địa chỉ không quá 255 ký tự")
    String address;

    LocalDate dob;

    // ---- Chỉ ADMIN được dùng các trường bên dưới ----
    @Email(message = "Email không hợp lệ")
    String email;

    /** Chỉ ADMIN mới được thay đổi role */
    Roles role;

    /** Chỉ ADMIN mới được thay đổi trạng thái tài khoản */
    UserStatus status;
}

