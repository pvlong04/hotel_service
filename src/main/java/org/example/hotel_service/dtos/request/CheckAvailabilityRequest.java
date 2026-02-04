package org.example.hotel_service.dtos.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu kiểm tra phòng trống
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckAvailabilityRequest {

    @NotNull(message = "Hotel ID không được để trống")
    private Integer hotelId;

    @NotNull(message = "Ngày check-in không được để trống")
    private LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    private LocalDate checkOutDate;

    @Builder.Default
    private Integer adults = 1;
    @Builder.Default
    private Integer children = 0;

    // Optional: chỉ kiểm tra loại phòng cụ thể
    private Long roomTypeId;
}
