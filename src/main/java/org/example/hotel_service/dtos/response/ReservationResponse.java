package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationResponse {
    Long reservationId;
    String reservationCode;
    String status;

    UUID guestId;
    String guestUsername;

    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer nightsCount;

    Integer adultCount;
    Integer childCount;
    String specialRequests;

    Long totalAmount;
    Long roomSubtotal;
    Long discountAmount;
    String promotionCode;
    Long paidAmount;

    String cancelReason;
    LocalDateTime confirmedAt;
    LocalDateTime checkedInAt;
    LocalDateTime checkedOutAt;
    LocalDateTime cancelledAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
        Long reservationItemId;
        Long roomId;
        String roomNumber;
        Long roomTypeId;
        String roomTypeCode;
        String roomTypeName;
        Long ratePerNight;
        Integer nights;
        Long amount;
        String status;
    }
}
