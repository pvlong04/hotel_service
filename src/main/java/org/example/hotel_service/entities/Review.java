package org.example.hotel_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    Long reviewId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    User guest;

    @Column(name = "rating", nullable = false)
    Integer rating;

    @Column(name = "title", length = 255)
    String title;

    @Column(name = "content", columnDefinition = "TEXT")
    String content;

    @Column(name = "cleanliness_rating")
    Integer cleanlinessRating;

    @Column(name = "service_rating")
    Integer serviceRating;

    @Column(name = "location_rating")
    Integer locationRating;

    @Column(name = "facilities_rating")
    Integer facilitiesRating;

    @Column(name = "value_rating")
    Integer valueRating;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    Boolean isVerified = true;

    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    Boolean isVisible = true;

    @Column(name = "reply", columnDefinition = "TEXT")
    String reply;

    @Column(name = "replied_at")
    LocalDateTime repliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by")
    User repliedBy;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isVerified == null) {
            isVerified = true;
        }
        if (isVisible == null) {
            isVisible = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

