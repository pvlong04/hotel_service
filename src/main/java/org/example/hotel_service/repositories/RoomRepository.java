package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Room;
import org.example.hotel_service.enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @EntityGraph(attributePaths = {"hotel", "roomType", "floor", "images"})
    Optional<Room> findWithDetailsByRoomId(Long roomId);

    @Query(value = "SELECT r FROM Room r LEFT JOIN FETCH r.roomType LEFT JOIN FETCH r.floor LEFT JOIN FETCH r.hotel",
            countQuery = "SELECT COUNT(r) FROM Room r")
    Page<Room> findAllBy(Pageable pageable);

    @Query(value = "SELECT r FROM Room r LEFT JOIN FETCH r.roomType LEFT JOIN FETCH r.floor LEFT JOIN FETCH r.hotel WHERE r.status = :status",
            countQuery = "SELECT COUNT(r) FROM Room r WHERE r.status = :status")
    Page<Room> findByStatus(@Param("status") RoomStatus status, Pageable pageable);

    boolean existsByRoomNumber(String roomNumber);

    boolean existsByRoomNumberAndRoomIdNot(String roomNumber, Long roomId);

    boolean existsByRoomType_RoomTypeId(Long roomTypeId);

    @EntityGraph(attributePaths = {"hotel", "roomType", "floor", "images"})
    @Query("""
            SELECT r FROM Room r
            WHERE r.status = 'AVAILABLE'
              AND r.roomId NOT IN (
                  SELECT ri.room.roomId FROM ReservationItem ri
                  JOIN ri.reservation res
                  WHERE res.status IN (
                        :#{T(org.example.hotel_service.enums.ReservationStatus).PENDING},
                        :#{T(org.example.hotel_service.enums.ReservationStatus).CONFIRMED},
                        :#{T(org.example.hotel_service.enums.ReservationStatus).CHECKED_IN}
                  )
                    AND res.checkInDate < :checkOut
                    AND res.checkOutDate > :checkIn
              )
            """)
    List<Room> findAvailableRooms(
            @Param("hotelId") Integer hotelId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
