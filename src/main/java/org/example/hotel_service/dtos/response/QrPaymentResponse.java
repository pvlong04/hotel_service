package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * DTO phản hồi tạo mã QR thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QrPaymentResponse {
    Long paymentId;
    Long reservationId;
    Long amount;
    String status;

    /** Nội dung QR – chuỗi mô phỏng thông tin chuyển khoản */
    String qrContent;

    /** Thời điểm hết hạn thanh toán QR */
    LocalDateTime expiresAt;

    LocalDateTime createdAt;
}
