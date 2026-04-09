package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Dashboard tổng quan cho admin/staff.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardOverviewResponse {
    Long totalUsers;
    Long totalRoomTypes;
    Long totalRooms;
    Long totalReservations;
}

