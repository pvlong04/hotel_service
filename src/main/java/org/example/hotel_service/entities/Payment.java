package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.PaymentMethod;
import org.example.hotel_service.enums.PaymentStatus;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "reservation_id")
    Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    User guest;

    @Column(name = "amount", nullable = false)
    Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    @Builder.Default
    PaymentMethod method = PaymentMethod.ONLINE;

    @Column(name = "provider", length = 100)
    String provider;

    @Column(name = "provider_trans_id", length = 100)
    String providerTransId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (method == null) {
            method = PaymentMethod.ONLINE;
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}
