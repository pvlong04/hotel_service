package org.example.hotel_service.services.payment;

import org.example.hotel_service.dtos.request.VnPayCreateRequest;
import org.example.hotel_service.dtos.response.PaymentResponse;
import org.example.hotel_service.dtos.response.VnPayCreateResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

public interface PaymentServiceImp {


    VnPayCreateResponse createVnPayPayment(VnPayCreateRequest request, String clientIp, Jwt jwt);

    PaymentResponse confirmVnPayPayment(Map<String, String> callbackParams, Jwt jwt);

    /**
     * Lấy chi tiết giao dịch
     */
    PaymentResponse getPaymentById(Long paymentId, Jwt jwt);
}
