package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.AmenityCategory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amenity_id")
    Integer amenityId;

    @Column(name = "name", nullable = false, unique = true, length = 120)
    String name;

    @Column(name = "description", length = 255)
    String description;

    /** Icon class hoặc URL icon để hiển thị trên UI */
    @Column(name = "icon", length = 100)
    String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    AmenityCategory category = AmenityCategory.ROOM;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    // Relationships
    @ManyToMany(mappedBy = "amenities", fetch = FetchType.LAZY)
    @Builder.Default
    List<RoomType> roomTypes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (category == null) category = AmenityCategory.ROOM;
    }
}
