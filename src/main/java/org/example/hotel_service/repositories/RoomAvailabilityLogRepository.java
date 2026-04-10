package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.RoomAvailabilityLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomAvailabilityLogRepository extends JpaRepository<RoomAvailabilityLog, Long> {

    @EntityGraph(attributePaths = {"room", "reservation", "changedBy"})
    List<RoomAvailabilityLog> findByRoom_RoomIdOrderByChangedAtDesc(Long roomId);

    @EntityGraph(attributePaths = {"room", "reservation", "changedBy"})
    List<RoomAvailabilityLog> findByReservation_ReservationIdOrderByChangedAtDesc(Long reservationId);
}

