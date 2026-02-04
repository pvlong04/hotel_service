package org.example.hotel_service.enums;

/**
 * Trạng thái đặt phòng
 */
public enum ReservationStatus {
    PENDING,        // Chờ xác nhận
    CONFIRMED,      // Đã xác nhận
    CHECKED_IN,     // Đã nhận phòng
    CHECKED_OUT,    // Đã trả phòng
    CANCELLED       // Đã hủy
}
