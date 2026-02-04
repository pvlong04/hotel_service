package org.example.hotel_service.dtos.response;

import java.math.BigDecimal;
import java.util.List;

import org.example.hotel_service.entities.RoomImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi tìm kiếm khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchResponse {
    private Integer hotelId;
    private String name;
    private String address;
    private Integer starRating;
    private String primaryImageUrl;
    private List<RoomImage> images;

    // Thống kê
    private BigDecimal averageRating;
    private Integer reviewCount;

    // Giá thấp nhất
    private Integer minPrice;
    private Integer maxPrice;

    // Phòng trống
    private Integer availableRooms;

    // Vị trí
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal distance; // km từ vị trí tìm kiếm
}
