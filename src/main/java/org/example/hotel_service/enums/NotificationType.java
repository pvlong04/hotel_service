package org.example.hotel_service.enums;

/**
 * Loại thông báo
 */
public enum NotificationType {
    RESERVATION_CREATED,    // Đơn đặt phòng mới
    RESERVATION_CONFIRMED,  // Đơn đã xác nhận
    RESERVATION_CHECKIN,    // Đã check-in
    RESERVATION_CHECKOUT,   // Đã check-out
    RESERVATION_CANCELLED,  // Đơn đã hủy
    RESERVATION_REMINDER,   // Nhắc nhở check-in
    PAYMENT_SUCCESS,        // Thanh toán thành công
    PAYMENT_FAILED,         // Thanh toán thất bại
    REVIEW_REQUEST,         // Yêu cầu đánh giá
    PROMOTION,              // Khuyến mãi
    SYSTEM                  // Thông báo hệ thống
}
