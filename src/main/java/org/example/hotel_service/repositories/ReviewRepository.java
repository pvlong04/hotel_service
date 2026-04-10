package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByReservation_ReservationId(Long reservationId);

    @EntityGraph(attributePaths = {"guest", "reservation", "repliedBy"})
    List<Review> findByIsVisibleTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"guest", "reservation", "repliedBy"})
    @Query("""
            SELECT DISTINCT review
            FROM Review review
            JOIN review.reservation reservation
            JOIN reservation.items item
            WHERE review.isVisible = true
              AND item.room.roomId = :roomId
            ORDER BY review.createdAt DESC
            """)
    List<Review> findPublicReviewsByRoomId(@Param("roomId") Long roomId);

    @EntityGraph(attributePaths = {"guest", "reservation", "repliedBy"})
    List<Review> findByGuest_UserIdOrderByCreatedAtDesc(UUID guestId);

    @EntityGraph(attributePaths = {"guest", "reservation", "repliedBy"})
    Optional<Review> findWithDetailsByReviewId(Long reviewId);
}

