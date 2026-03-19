package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.ReservationItemStatus;

import java.time.LocalDateTime;

/**
 * Chi tiết từng phòng trong một đơn đặt.
 * Một đơn đặt có thể gồm nhiều phòng (group booking).
 * Ghi lại giá tại thời điểm đặt để tránh bị ảnh hưởng khi giá phòng thay đổi.
 */
@Entity
@Table(name = "reservation_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"reservation_id", "room_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_item_id")
    Long reservationItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    Room room;

    /** Snapshot loại phòng tại thời điểm đặt */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    RoomType roomType;

    /** Giá mỗi đêm tại thời điểm đặt (VND) – không thay đổi dù giá phòng cập nhật sau */
    @Column(name = "rate_per_night", nullable = false)
    @Builder.Default
    Long ratePerNight = 0L;

    @Column(name = "nights", nullable = false)
    @Builder.Default
    Integer nights = 1;

    /** Tổng tiền của dòng này = ratePerNight × nights */
    @Column(name = "amount", nullable = false)
    @Builder.Default
    Long amount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    ReservationItemStatus status = ReservationItemStatus.BOOKED;

    /** Thời điểm check-in thực tế của phòng này */
    @Column(name = "checked_in_at")
    LocalDateTime checkedInAt;

    /** Thời điểm check-out thực tế của phòng này */
    @Column(name = "checked_out_at")
    LocalDateTime checkedOutAt;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ReservationItemStatus.BOOKED;
        if (ratePerNight == null) ratePerNight = 0L;
        if (nights == null) nights = 1;
        if (amount == null) amount = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
