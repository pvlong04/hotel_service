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

/**
 * DTO phản hồi tìm kiếm phòng trống
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilityResponse {
    private Integer hotelId;
    private String hotelName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer nights;
    private List<AvailableRoomType> roomTypes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableRoomType {
        private Long roomTypeId;
        private String code;
        private String name;
        private String description;
        private Integer capacity;
        private Integer maxAdults;
        private Integer maxChildren;
        private BedType bedType;
        private BigDecimal roomSize;

        // Giá
        private Integer pricePerNight;
        private Integer totalPrice;

        // Số phòng trống
        private Integer availableRooms;

        // Hình ảnh và tiện nghi
        private String primaryImageUrl;
        private List<RoomType> images;
        private List<Amenity> amenities;
    }
}
