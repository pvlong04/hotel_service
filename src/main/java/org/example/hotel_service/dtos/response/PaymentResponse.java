package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    Long paymentId;
    Long reservationId;
    UUID guestId;

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

