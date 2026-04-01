package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.HotelImageType;

import java.time.LocalDateTime;

/**
 * Hình ảnh của khách sạn (sảnh, hồ bơi, ngoại thất...)
 */
@Entity
@Table(name = "hotel_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HotelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    Long imageId;


    @Column(name = "url", nullable = false, length = 500)
    String url;

    @Column(name = "caption", length = 255)
    String caption;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    HotelImageType type = HotelImageType.OTHER;

    /** Ảnh đại diện của khách sạn */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    Boolean isPrimary = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isPrimary == null) isPrimary = false;
        if (sortOrder == null) sortOrder = 0;
    }
}

