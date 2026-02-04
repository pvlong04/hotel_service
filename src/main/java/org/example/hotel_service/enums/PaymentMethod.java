package org.example.hotel_service.enums;

/**
 * Phương thức thanh toán
 */
public enum PaymentMethod {
    CARD,           // Thẻ tín dụng/ghi nợ
    CASH,           // Tiền mặt
    BANK_TRANSFER,  // Chuyển khoản
    E_WALLET,       // Ví điện tử (Momo, ZaloPay...)
    ONLINE          // Thanh toán online khác
}
