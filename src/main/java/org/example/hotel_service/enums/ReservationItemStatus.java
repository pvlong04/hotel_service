package org.example.hotel_service.enums;

/**
 * Trạng thái chi tiết phòng trong đơn đặt
 */
public enum ReservationItemStatus {
    BOOKED,         // Đã đặt
    CHECKED_IN,     // Đã nhận phòng
    CHECKED_OUT,    // Đã trả phòng
    CANCELLED       // Đã hủy
}
