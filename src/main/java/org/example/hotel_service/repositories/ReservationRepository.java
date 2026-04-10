package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Reservation;
import org.example.hotel_service.enums.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {
            "guest", "items", "items.room", "items.roomType", "payments", "charges"
    })
    Optional<Reservation> findWithDetailsByReservationId(Long reservationId);

    @EntityGraph(attributePaths = {"items", "items.room", "items.roomType"})
    List<Reservation> findByGuest_UserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"guest", "items", "items.room", "items.roomType"})
    List<Reservation> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"guest", "items", "items.room", "items.roomType"})
    List<Reservation> findByStatusOrderByCreatedAtDesc(ReservationStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(ri) > 0 THEN true ELSE false END
            FROM ReservationItem ri
            JOIN ri.reservation res
            WHERE ri.room.roomId = :roomId
              AND res.status IN (
                    :#{T(org.example.hotel_service.enums.ReservationStatus).PENDING},
                    :#{T(org.example.hotel_service.enums.ReservationStatus).CONFIRMED},
                    :#{T(org.example.hotel_service.enums.ReservationStatus).CHECKED_IN}
              )
              AND res.checkInDate < :checkOut
              AND res.checkOutDate > :checkIn
            """)
    boolean existsActiveOverlapByRoomId(@Param("roomId") Long roomId,
                                        @Param("checkIn") LocalDate checkIn,
                                        @Param("checkOut") LocalDate checkOut);
}

