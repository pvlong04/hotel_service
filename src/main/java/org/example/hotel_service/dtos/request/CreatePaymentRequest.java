package org.example.hotel_service.dtos.request;

import org.example.hotel_service.enums.PaymentMethod;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentRequest {

    @NotNull(message = "Reservation ID không được để trống")
    Long reservationId;

    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 1, message = "Số tiền phải lớn hơn 0")
    Integer amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    PaymentMethod method;

    @Size(max = 100, message = "Provider không quá 100 ký tự")
    String provider;

    @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
    String notes;
}
