package org.example.hotel_service.dtos.request;

import java.time.LocalDate;

import org.example.hotel_service.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu cập nhật profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 120, message = "Họ tên không quá 120 ký tự")
    String fullName;

    @Size(max = 30, message = "Số điện thoại không quá 30 ký tự")
    String phone;

    @Size(max = 500, message = "URL avatar không quá 500 ký tự")
    String avatarUrl;

    @Size(max = 255, message = "Địa chỉ không quá 255 ký tự")
    String address;

    LocalDate dob;

    Gender gender;

    @Size(max = 50, message = "Số CMND/CCCD không quá 50 ký tự")
    String idCardNumber;

    @Size(max = 100, message = "Quốc tịch không quá 100 ký tự")
    String nationality;
}
