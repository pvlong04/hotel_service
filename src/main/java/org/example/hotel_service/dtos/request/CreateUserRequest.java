package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.Roles;

import java.time.LocalDate;

/**
 * DTO yêu cầu tạo mới người dùng (chỉ ADMIN)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3-50 ký tự")
    String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu phải từ 6-100 ký tự")
    String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 120, message = "Họ tên không quá 120 ký tự")
    String fullName;

    @Size(max = 30, message = "Số điện thoại không quá 30 ký tự")
    String phone;

    @Size(max = 255, message = "Địa chỉ không quá 255 ký tự")
    String address;

    LocalDate dob;

    @NotNull(message = "Vai trò không được để trống")
    Roles role;
}

