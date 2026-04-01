package org.example.hotel_service.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.VnPayCreateRequest;
import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.VnPayCreateResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    PaymentServiceImp paymentService;

    @PostMapping("/vnpay/create")
    public ResponseEntity<ApiResponse<VnPayCreateResponse>> createVnPayPayment(
            @Valid @RequestBody VnPayCreateRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = httpServletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = httpServletRequest.getRemoteAddr();
        } else if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        VnPayCreateResponse result = paymentService.createVnPayPayment(
                request, clientIp, jwt
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thanh toán VNPay thành công", result));
    }

    @PostMapping("/vnpay/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmVnPayPayment(
            @RequestBody Map<String, String> callbackParams,
            @AuthenticationPrincipal Jwt jwt
    ) {
        PaymentResponse result = paymentService.confirmVnPayPayment(callbackParams, jwt);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán VNPay thành công", result));
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

}
