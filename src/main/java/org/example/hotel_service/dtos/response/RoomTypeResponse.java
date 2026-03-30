package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeResponse {
    Long roomTypeId;
    String code;
    String name;
    String description;
    Integer capacity;
    Integer maxAdults;
    Integer maxChildren;
    Long pricePerNight;
    Long weekendPrice;
    Long extraBedPrice;
    String bedType;
    Integer bedCount;
    BigDecimal roomSize;
    String status;
    Integer totalRooms;
    Integer availableRooms;
    List<AmenityItem> amenities;
    List<ImageItem> images;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityItem {
        Integer amenityId;
        String name;
        String category;
        String icon;
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
}

