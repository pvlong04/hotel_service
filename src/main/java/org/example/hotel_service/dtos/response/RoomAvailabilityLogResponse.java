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
public class RoomAvailabilityLogResponse {

    Long logId;

    Long roomId;
    String roomNumber;

    String oldStatus;
    String newStatus;
    String reason;

    Long reservationId;
    String reservationCode;

    UUID changedByUserId;
    String changedByUsername;
    LocalDateTime changedAt;
}

