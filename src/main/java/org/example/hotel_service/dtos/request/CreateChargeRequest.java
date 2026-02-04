package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.ChargeType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu thêm chi phí phát sinh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChargeRequest {

    @NotNull(message = "Reservation ID không được để trống")
    private Long reservationId;

    @NotNull(message = "Loại chi phí không được để trống")
    private ChargeType chargeType;

    @Size(max = 255, message = "Mô tả không quá 255 ký tự")
    private String description;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    @Builder.Default
    private Integer quantity = 1;

    @NotNull(message = "Đơn giá không được để trống")
    @Min(value = 0, message = "Đơn giá không được âm")
    private Integer unitPrice;
}
