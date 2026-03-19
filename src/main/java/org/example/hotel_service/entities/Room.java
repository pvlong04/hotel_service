package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.RoomStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phòng vật lý trong khách sạn.
 * Mỗi phòng thuộc một loại phòng (RoomType) và một tầng (Floor).
 * Ảnh gallery riêng của phòng (nếu có, thường dùng ảnh của RoomType).
 */
@Entity
@Table(name = "rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hotel_id", "room_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    Hotel hotel;

    /** Số phòng hiển thị (101, 201A, P3...) */
    @Column(name = "room_number", nullable = false, length = 50)
    String roomNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    Floor floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    RoomStatus status = RoomStatus.AVAILABLE;

    /** Ghi chú nội bộ về phòng (ví dụ: view biển, gần thang máy) */
    @Column(name = "note", length = 500)
    String note;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships – ảnh riêng của phòng (tùy chọn, thường kế thừa từ RoomType)
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<RoomImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = RoomStatus.AVAILABLE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
