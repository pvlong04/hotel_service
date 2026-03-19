package org.example.hotel_service.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

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
    String status;

    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer nightsCount;

    Long totalAmount;
    Long paidAmount;

    String paymentUrl;
    String message;
}
