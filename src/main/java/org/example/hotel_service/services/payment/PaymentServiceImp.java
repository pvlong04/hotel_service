package org.example.hotel_service.services.payment;

import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.QrPaymentResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface PaymentServiceImp {

    /**
     * Tạo giao dịch PENDING và trả về nội dung QR mô phỏng
     */
    QrPaymentResponse generateQrPayment(Long reservationId, Long amount, Jwt jwt);

    /**
     * Xác nhận thanh toán QR – mô phỏng callback từ cổng thanh toán
     */
    PaymentResponse confirmQrPayment(Long paymentId, Jwt jwt);

    /**
     * Lấy chi tiết giao dịch
     */
    PaymentResponse getPaymentById(Long paymentId, Jwt jwt);
}
