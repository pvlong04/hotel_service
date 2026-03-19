package org.example.hotel_service.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    User user;

    @Column(name = "full_name", nullable = false, length = 120)
    String fullName;

    @Column(name = "phone", unique = true, length = 30)
    String phone;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Column(name = "address", length = 500)
    String address;

    @Column(name = "dob")
    LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    Gender gender;

    /** Số CCCD / Hộ chiếu – dùng khi check-in theo quy định khách sạn */
    @Column(name = "national_id", length = 50)
    String nationalId;

    /** Quốc tịch */
    @Column(name = "nationality", length = 100)
    String nationality;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
