package org.example.hotel_service.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * DTO thống kê dashboard cho Admin/Staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatsResponse {
    // Tổng quan
    Integer totalRooms;
    Integer availableRooms;
    Integer occupiedRooms;
    Integer maintenanceRooms;
    BigDecimal occupancyRate; // Tỷ lệ lấp đầy (%)

    // Đặt phòng
    Integer todayCheckIns;
    Integer todayCheckOuts;
    Integer pendingReservations;
    Integer confirmedReservations;

    // Doanh thu
    Long todayRevenue;
    Long monthRevenue;
    Long yearRevenue;

    // Khách hàng
    Integer totalGuests;
    Integer newGuestsThisMonth;

    // Đánh giá
    BigDecimal averageRating;
    Integer totalReviews;
    Integer newReviewsThisMonth;
}
