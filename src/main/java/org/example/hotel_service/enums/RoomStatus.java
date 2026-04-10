package org.example.hotel_service.enums;

/**
 * Trạng thái phòng
 */
public enum RoomStatus {
    AVAILABLE,      // Trống, sẵn sàng đặt
    OCCUPIED,       // Đang có khách
    MAINTENANCE,    // Đang bảo trì
    REMOVED         // Đã ngừng sử dụng
}
