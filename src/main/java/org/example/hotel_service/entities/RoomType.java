package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.hotel_service.enums.RoomTypeStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hotel_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    private Long roomTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 1;

    @Column(name = "price_per_night", nullable = false)
    @Builder.Default
    private Integer pricePerNight = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RoomTypeStatus status = RoomTypeStatus.ACTIVE;

    @Column(name = "total_rooms", nullable = false)
    @Builder.Default
    private Integer totalRooms = 0;

    @Column(name = "available_rooms", nullable = false)
    @Builder.Default
    private Integer availableRooms = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Room> rooms = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private List<Amenity> amenities = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = RoomTypeStatus.ACTIVE;
        }
        if (capacity == null) {
            capacity = 1;
        }
        if (pricePerNight == null) {
            pricePerNight = 0;
        }
        if (totalRooms == null) {
            totalRooms = 0;
        }
        if (availableRooms == null) {
            availableRooms = 0;
        }
    }
}
