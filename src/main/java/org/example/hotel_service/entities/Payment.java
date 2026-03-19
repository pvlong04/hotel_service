package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.PaymentMethod;
import org.example.hotel_service.enums.PaymentStatus;

import java.time.LocalDateTime;

/**
 * Giao dịch thanh toán cho một đơn đặt phòng.
 * Một đơn có thể có nhiều giao dịch (đặt cọc + thanh toán phần còn lại).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    User guest;

    /** Số tiền giao dịch này (VND) */
    @Column(name = "amount", nullable = false)
    Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    @Builder.Default
    PaymentMethod method = PaymentMethod.CASH;

    /** Tên cổng thanh toán (VNPay, Momo, ZaloPay...) */
    @Column(name = "provider", length = 100)
    String provider;

    /** Mã giao dịch từ cổng thanh toán */
    @Column(name = "provider_trans_id", length = 150)
    String providerTransId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    PaymentStatus status = PaymentStatus.PENDING;

    /** Ghi chú (nhân viên nhập khi thu tiền mặt) */
    @Column(name = "note", length = 500)
    String note;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (method == null) method = PaymentMethod.CASH;
        if (status == null) status = PaymentStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
