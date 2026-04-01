package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.BedType;
import org.example.hotel_service.enums.RoomTypeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Loại phòng (Standard, Deluxe, Suite...)
 * Mỗi loại phòng chứa thông tin về giá, sức chứa, tiện nghi,
 * số phòng hiện có và hình ảnh đại diện.
 */
@Entity
@Table(name = "room_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    Long roomTypeId;


    /** Mã định danh ngắn (STD, DLX, SUT...) */
    @Column(name = "code", nullable = false, length = 50)
    String code;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    /** Tổng sức chứa (người lớn + trẻ em) */
    @Column(name = "capacity", nullable = false)
    @Builder.Default
    Integer capacity = 2;

    /** Số người lớn tối đa */
    @Column(name = "max_adults", nullable = false)
    @Builder.Default
    Integer maxAdults = 2;

    /** Số trẻ em tối đa */
    @Column(name = "max_children", nullable = false)
    @Builder.Default
    Integer maxChildren = 0;

    /** Giá cơ bản mỗi đêm (VND) */
    @Column(name = "price_per_night", nullable = false)
    @Builder.Default
    Long pricePerNight = 0L;

    /** Giá cuối tuần (Thứ 6, Thứ 7, Chủ nhật) */
    @Column(name = "weekend_price")
    Long weekendPrice;

    /** Phí giường phụ */
    @Column(name = "extra_bed_price")
    Long extraBedPrice;

    /** Loại giường */
    @Enumerated(EnumType.STRING)
    @Column(name = "bed_type", length = 20)
    BedType bedType;

    /** Số giường trong phòng */
    @Column(name = "bed_count", nullable = false)
    @Builder.Default
    Integer bedCount = 1;

    /** Diện tích phòng (m²) */
    @Column(name = "room_size", precision = 6, scale = 2)
    BigDecimal roomSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    RoomTypeStatus status = RoomTypeStatus.ACTIVE;

    /** Tổng số phòng thuộc loại này (cập nhật bởi trigger) */
    @Column(name = "total_rooms", nullable = false)
    @Builder.Default
    Integer totalRooms = 0;

    /** Số phòng đang trống (cập nhật bởi trigger) */
    @Column(name = "available_rooms", nullable = false)
    @Builder.Default
    Integer availableRooms = 0;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<Room> rooms = new ArrayList<>();

    /** Hình ảnh của loại phòng này (thumbnail, gallery) */
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<RoomTypeImage> images = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    List<Amenity> amenities = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = RoomTypeStatus.ACTIVE;
        if (capacity == null) capacity = 2;
        if (maxAdults == null) maxAdults = 2;
        if (maxChildren == null) maxChildren = 0;
        if (pricePerNight == null) pricePerNight = 0L;
        if (totalRooms == null) totalRooms = 0;
        if (availableRooms == null) availableRooms = 0;
        if (bedCount == null) bedCount = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
