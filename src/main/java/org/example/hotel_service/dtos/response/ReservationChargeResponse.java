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
public class ReservationChargeResponse {
    Long chargeId;
    Long reservationId;

    String chargeType;
    String description;
    Long amount;

    Long createdByUserId;
    String createdByUsername;
    LocalDateTime createdAt;
}

