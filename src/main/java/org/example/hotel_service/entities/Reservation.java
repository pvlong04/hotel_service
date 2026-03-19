package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn đặt phòng (Booking/Reservation).
 *
 * Luồng trạng thái:
 *   PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
 *                      ↘ CANCELLED (có thể hủy ở PENDING hoặc CONFIRMED)
 *
 * check_in_date / check_out_date: ngày khách yêu cầu (LocalDate).
 * checked_in_at / checked_out_at: thời điểm thực tế nhận/trả phòng.
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    Long reservationId;

    /** Mã đặt phòng hiển thị cho khách (BK20241105-XXXX) */
    @Column(name = "reservation_code", nullable = false, unique = true, length = 40)
    String reservationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    User guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    ReservationStatus status = ReservationStatus.PENDING;

    /** Ngày check-in theo yêu cầu của khách */
    @Column(name = "check_in_date", nullable = false)
    LocalDate checkInDate;

    /** Ngày check-out theo yêu cầu của khách */
    @Column(name = "check_out_date", nullable = false)
    LocalDate checkOutDate;

    /** Số đêm = checkOutDate - checkInDate */
    @Column(name = "nights_count", nullable = false)
    @Builder.Default
    Integer nightsCount = 1;

    /** Số người lớn */
    @Column(name = "adult_count", nullable = false)
    @Builder.Default
    Integer adultCount = 1;

    /** Số trẻ em */
    @Column(name = "child_count", nullable = false)
    @Builder.Default
    Integer childCount = 0;

    /** Yêu cầu đặc biệt của khách (view biển, tầng cao, giường phụ...) */
    @Column(name = "special_requests", columnDefinition = "TEXT")
    String specialRequests;

    /** Tổng tiền phòng + phụ phí (VND) */
    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    Long totalAmount = 0L;

    /** Số tiền đã thanh toán */
    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    Long paidAmount = 0L;

    /** Nhân viên/admin xác nhận đơn */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    User confirmedBy;

    /** Thời điểm xác nhận đơn */
    @Column(name = "confirmed_at")
    LocalDateTime confirmedAt;

    /** Thời điểm check-in thực tế */
    @Column(name = "checked_in_at")
    LocalDateTime checkedInAt;

    /** Thời điểm check-out thực tế */
    @Column(name = "checked_out_at")
    LocalDateTime checkedOutAt;

    /** Lý do hủy đơn */
    @Column(name = "cancel_reason", length = 500)
    String cancelReason;

    /** Ai hủy: khách hay nhân viên */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    User cancelledBy;

    @Column(name = "cancelled_at")
    LocalDateTime cancelledAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<ReservationItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<ReservationCharge> charges = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ReservationStatus.PENDING;
        if (totalAmount == null) totalAmount = 0L;
        if (paidAmount == null) paidAmount = 0L;
        if (adultCount == null) adultCount = 1;
        if (childCount == null) childCount = 0;
        if (nightsCount == null) nightsCount = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
