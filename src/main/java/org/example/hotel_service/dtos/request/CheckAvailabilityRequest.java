package org.example.hotel_service.dtos.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu kiểm tra phòng trống
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckAvailabilityRequest {


    @NotNull(message = "Ngày check-in không được để trống")
    LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    LocalDate checkOutDate;

    @Builder.Default
    Integer adults = 1;
    @Builder.Default
    Integer children = 0;

    // Optional: chỉ kiểm tra loại phòng cụ thể
    Long roomTypeId;
}
