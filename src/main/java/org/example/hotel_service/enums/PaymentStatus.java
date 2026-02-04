package org.example.hotel_service.enums;

/**
 * Trạng thái thanh toán
 */
public enum PaymentStatus {
    PENDING,            // Chờ xử lý
    COMPLETED,          // Hoàn thành
    FAILED,             // Thất bại
    REFUNDED,           // Đã hoàn tiền
    PARTIALLY_REFUNDED  // Hoàn tiền một phần
}
