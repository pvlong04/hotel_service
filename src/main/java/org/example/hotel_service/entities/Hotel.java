package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.HotelStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hotel_id")
    Integer hotelId;

    @Column(name = "name", nullable = false, length = 255)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "address", length = 500)
    String address;

    @Column(name = "latitude", precision = 10, scale = 8)
    BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    BigDecimal longitude;

    @Column(name = "city", length = 100)
    String city;

    @Column(name = "country", length = 100)
    String country;

    @Column(name = "phone", length = 64)
    String phone;

    @Column(name = "email", length = 150)
    String email;

    @Column(name = "website", length = 255)
    String website;

    /** Số sao khách sạn (1-5) */
    @Column(name = "star_rating")
    Integer starRating;

    /** Giờ nhận phòng mặc định, ví dụ 14:00 */
    @Column(name = "check_in_time")
    LocalTime checkInTime;

    /** Giờ trả phòng mặc định, ví dụ 12:00 */
    @Column(name = "check_out_time")
    LocalTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    HotelStatus status = HotelStatus.ACTIVE;

    @Column(name = "timezone", length = 64)
    String timezone;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = HotelStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
