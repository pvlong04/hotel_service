package org.example.hotel_service.dtos.request;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.example.hotel_service.enums.HotelStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu tạo/cập nhật khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelRequest {

    @NotBlank(message = "Tên khách sạn không được để trống")
    @Size(max = 255, message = "Tên khách sạn không quá 255 ký tự")
    String name;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    String address;

    @Size(max = 64, message = "Số điện thoại không quá 64 ký tự")
    String phone;

    @Size(max = 150, message = "Email không quá 150 ký tự")
    String email;

    String description;

    @Min(value = 1, message = "Số sao tối thiểu là 1")
    @Max(value = 5, message = "Số sao tối đa là 5")
    Integer starRating;

    LocalTime checkInTime;
    LocalTime checkOutTime;

    BigDecimal latitude;
    BigDecimal longitude;

    @Size(max = 64, message = "Timezone không quá 64 ký tự")
    String timezone;

    HotelStatus status;
}
