package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.ChargeType;

import java.time.LocalDateTime;

/**
 * Phụ phí phát sinh trong quá trình lưu trú.
 * Ví dụ: minibar, giặt đồ, phí hủy muộn, giường phụ, hư hỏng...
 */
@Entity
@Table(name = "reservation_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charge_id")
    Long chargeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false)
    @Builder.Default
    ChargeType chargeType = ChargeType.OTHER;

    @Column(name = "description", length = 500)
    String description;

    /** Số tiền phụ phí (VND) */
    @Column(name = "amount", nullable = false)
    @Builder.Default
    Long amount = 0L;

    /** Nhân viên ghi nhận phụ phí */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    User createdBy;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (chargeType == null) chargeType = ChargeType.OTHER;
        if (amount == null) amount = 0L;
    }
}
