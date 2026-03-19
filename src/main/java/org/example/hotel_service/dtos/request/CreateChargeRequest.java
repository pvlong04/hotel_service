package org.example.hotel_service.dtos.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.ChargeType;

/**
 * Thêm phụ phí phát sinh trong kỳ lưu trú.
 * Chỉ STAFF / ADMIN thực hiện.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateChargeRequest {

    @NotNull(message = "Reservation ID không được để trống")
    Long reservationId;

    @NotNull(message = "Loại phụ phí không được để trống")
    ChargeType chargeType;

    @Size(max = 500, message = "Mô tả không quá 500 ký tự")
    String description;

    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 1, message = "Số tiền phụ phí phải lớn hơn 0")
    Long amount;
}
