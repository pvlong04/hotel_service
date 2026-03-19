package org.example.hotel_service.dtos.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO phản hồi tìm kiếm phòng trống.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAvailabilityResponse {

    Integer hotelId;
    String hotelName;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer nights;
    List<AvailableRoomType> roomTypes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AvailableRoomType {
        Long roomTypeId;
        String code;
        String name;
        String description;
        Integer capacity;
        Integer maxAdults;
        Integer maxChildren;
        String bedType;
        Integer bedCount;
        BigDecimal roomSize;

        Long pricePerNight;
        Long totalPrice;

        Integer totalRooms;
        Integer availableRooms;

        String primaryImageUrl;
        List<ImageItem> images;
        List<AmenityItem> amenities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ImageItem {
        Long imageId;
        String url;
        String caption;
        Boolean isPrimary;
        Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityItem {
        Integer amenityId;
        String name;
        String description;
        String icon;
        String category;
    }
}
