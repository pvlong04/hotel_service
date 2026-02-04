package org.example.hotel_service.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO phản hồi tạo đặt phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationCreatedResponse {
    Long reservationId;
    String reservationCode;
    Integer totalAmount;
    String status;
    String paymentUrl; // URL thanh toán (nếu có)
    String message;
}
