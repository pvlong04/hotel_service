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
public class PaymentResponse {
    Long paymentId;
    Long reservationId;
    Long guestId;

    Long amount;
    String method;
    String provider;
    String providerTransId;
    String status;
    String note;

    LocalDateTime paidAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

