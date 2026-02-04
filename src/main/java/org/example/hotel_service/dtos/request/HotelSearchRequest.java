package org.example.hotel_service.dtos.request;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO yêu cầu tìm kiếm khách sạn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelSearchRequest {

    String keyword; // Tìm theo tên, địa chỉ

    LocalDate checkInDate;
    LocalDate checkOutDate;

    Integer adults;
    Integer children;
    Integer rooms; // Số phòng cần

    // Filter
    Integer minStarRating;
    Integer maxStarRating;
    Integer minPrice;
    Integer maxPrice;

    // Location
    Double latitude;
    Double longitude;
    Double radiusKm; // Bán kính tìm kiếm

    // Sort
    String sortBy; // price, rating, distance, name
    String sortDirection; // asc, desc

    // Pagination
    @Builder.Default
    Integer page = 0;
    @Builder.Default
    Integer size = 10;
}
