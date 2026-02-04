package org.example.hotel_service.dtos.response;

import java.math.BigDecimal;
import java.util.List;

import org.example.hotel_service.entities.RoomImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO phản hồi tìm kiếm khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelSearchResponse {
    Integer hotelId;
    String name;
    String address;
    Integer starRating;
    String primaryImageUrl;
    List<RoomImage> images;

    // Thống kê
    BigDecimal averageRating;
    Integer reviewCount;

    // Giá thấp nhất
    Integer minPrice;
    Integer maxPrice;

    // Phòng trống
    Integer availableRooms;

    // Vị trí
    BigDecimal latitude;
    BigDecimal longitude;
    BigDecimal distance; // km từ vị trí tìm kiếm
}
