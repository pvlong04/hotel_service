package org.example.hotel_service.dtos.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO thống kê dashboard cho Admin/Staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    // Tổng quan
    private Integer totalRooms;
    private Integer availableRooms;
    private Integer occupiedRooms;
    private Integer maintenanceRooms;
    private BigDecimal occupancyRate; // Tỷ lệ lấp đầy (%)

    // Đặt phòng
    private Integer todayCheckIns;
    private Integer todayCheckOuts;
    private Integer pendingReservations;
    private Integer confirmedReservations;

    // Doanh thu
    private Long todayRevenue;
    private Long monthRevenue;
    private Long yearRevenue;

    // Khách hàng
    private Integer totalGuests;
    private Integer newGuestsThisMonth;

    // Đánh giá
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer newReviewsThisMonth;
}
