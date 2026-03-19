package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Hình ảnh của loại phòng (gallery phòng Standard, Deluxe, Suite...)
 * Tách khỏi room_images để ảnh dùng chung cho cả loại phòng,
 * không bị trùng khi có nhiều phòng cùng loại.
 */
@Entity
@Table(name = "room_type_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    RoomType roomType;

    @Column(name = "url", nullable = false, length = 500)
    String url;

    @Column(name = "caption", length = 255)
    String caption;

    /** Ảnh thumbnail chính của loại phòng */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    Boolean isPrimary = false;

    /** Thứ tự hiển thị */
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

