package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.ReservationItemStatus;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    RoomType roomType;

    @Column(name = "rate_per_night", nullable = false)
    @Builder.Default
    Integer ratePerNight = 0;

    @Column(name = "nights", nullable = false)
    @Builder.Default
    Integer nights = 1;

    @Column(name = "amount", nullable = false)
    @Builder.Default
    Integer amount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    ReservationItemStatus status = ReservationItemStatus.BOOKED;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ReservationItemStatus.BOOKED;
        }
        if (ratePerNight == null) {
            ratePerNight = 0;
        }
        if (nights == null) {
            nights = 1;
        }
        if (amount == null) {
            amount = 0;
        }
    }
}
