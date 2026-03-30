package org.example.hotel_service.controllers;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.QrPaymentResponse;
import org.example.hotel_service.services.payment.PaymentServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    PaymentServiceImp paymentService;

    /**
     * Tạo mã QR thanh toán mô phỏng cho đơn đặt phòng.
     * POST /payments/qr/generate
     * Body: { "reservationId": 1, "amount": 500000 }
     */
    @PostMapping("/qr/generate")
    public ResponseEntity<ApiResponse<QrPaymentResponse>> generateQrPayment(
            @RequestBody QrGenerateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        QrPaymentResponse result = paymentService.generateQrPayment(
                request.getReservationId(), request.getAmount(), jwt
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã QR thanh toán thành công", result));
    }

    /**
     * Xác nhận thanh toán QR – mô phỏng callback thành công từ cổng thanh toán.
     * POST /payments/qr/confirm/{paymentId}
     */
    @PostMapping("/qr/confirm/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmQrPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        PaymentResponse result = paymentService.confirmQrPayment(paymentId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Thanh toán QR thành công", result));
    }

    /**
     * Lấy chi tiết giao dịch thanh toán.
     * GET /payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        PaymentResponse result = paymentService.getPaymentById(paymentId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết thanh toán thành công", result));
    }

    // ========================  Inner Request DTO  ========================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QrGenerateRequest {
        @NotNull(message = "Reservation ID không được để trống")
        Long reservationId;

        @NotNull(message = "Số tiền không được để trống")
        @Min(value = 1, message = "Số tiền phải lớn hơn 0")
        Long amount;
    }
}
