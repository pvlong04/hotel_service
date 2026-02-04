package org.example.hotel_service.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.example.hotel_service.entities.Amenity;
import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.enums.BedType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO phản hồi tìm kiếm phòng trống
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
        BedType bedType;
        BigDecimal roomSize;

        // Giá
        Integer pricePerNight;
        Integer totalPrice;

        // Số phòng trống
        Integer availableRooms;

        // Hình ảnh và tiện nghi
        String primaryImageUrl;
        List<RoomType> images;
        List<Amenity> amenities;
    }
}
