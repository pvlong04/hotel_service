package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.Gender;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO yêu cầu cập nhật người dùng (ADMIN: full; STAFF/GUEST: chỉ profile)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {

    @Size(max = 120, message = "Họ tên không quá 120 ký tự")
    String fullName;

    @Size(max = 30, message = "Số điện thoại không quá 30 ký tự")
    String phone;

    @Size(max = 500, message = "URL avatar không quá 500 ký tự")
    String avatarUrl;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    String address;

    LocalDate dob;

    Gender gender;

    @Size(max = 50, message = "Số giấy tờ không quá 50 ký tự")
    String nationalId;

    @Size(max = 100, message = "Quốc tịch không quá 100 ký tự")
    String nationality;

    @Email(message = "Email không hợp lệ")
    String email;

    Set<Roles> roles;

    UserStatus status;
}
