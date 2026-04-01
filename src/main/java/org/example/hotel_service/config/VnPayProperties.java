package org.example.hotel_service.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnPayProperties {
    String tmnCode;
    String hashSecret;
    String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    String returnUrl = "http://localhost:3000/payment/vnpay-return";
    String version = "2.1.0";
    String command = "pay";
    String currCode = "VND";
    String locale = "vn";
    String orderType = "other";
    String bankCode = "VNPAYQR";
}

