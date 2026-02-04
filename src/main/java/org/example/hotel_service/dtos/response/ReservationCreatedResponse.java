package org.example.hotel_service.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi tạo đặt phòng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreatedResponse {
    private Long reservationId;
    private String reservationCode;
    private Integer totalAmount;
    private String status;
    private String paymentUrl; // URL thanh toán (nếu có)
    private String message;
}
