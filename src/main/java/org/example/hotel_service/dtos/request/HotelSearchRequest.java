package org.example.hotel_service.dtos.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu tìm kiếm khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchRequest {

    private String keyword; // Tìm theo tên, địa chỉ

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private Integer adults;
    private Integer children;
    private Integer rooms; // Số phòng cần

    // Filter
    private Integer minStarRating;
    private Integer maxStarRating;
    private Integer minPrice;
    private Integer maxPrice;

    // Location
    private Double latitude;
    private Double longitude;
    private Double radiusKm; // Bán kính tìm kiếm

    // Sort
    private String sortBy; // price, rating, distance, name
    private String sortDirection; // asc, desc

    // Pagination
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
}
