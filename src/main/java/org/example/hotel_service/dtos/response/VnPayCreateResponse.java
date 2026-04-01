package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VnPayCreateResponse {
    Long paymentId;
    Long reservationId;
    Long amount;
    String status;
    String txnRef;
    String paymentUrl;
    String qrContent;
    String qrImage;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;
}

