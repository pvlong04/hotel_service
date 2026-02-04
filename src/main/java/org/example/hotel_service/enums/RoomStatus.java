package org.example.hotel_service.enums;

/**
 * Trạng thái phòng
 */
public enum RoomStatus {
    AVAILABLE,      // Trống, sẵn sàng đặt
    HELD,           // Đang giữ chỗ (chờ thanh toán)
    OCCUPIED,       // Đang có khách
    MAINTENANCE,    // Đang bảo trì
    REMOVED         // Đã ngừng sử dụng
}
