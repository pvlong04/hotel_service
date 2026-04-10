package org.example.hotel_service.dtos.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {

    Long reviewId;
    Long reservationId;
    String reservationCode;

    UUID guestId;
    String guestUsername;

    Integer rating;
    String title;
    String content;

    Integer cleanlinessRating;
    Integer serviceRating;
    Integer locationRating;
    Integer facilitiesRating;
    Integer valueRating;

    Boolean isVerified;
    Boolean isVisible;

    String reply;
    LocalDateTime repliedAt;
    UUID repliedByUserId;
    String repliedByUsername;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

