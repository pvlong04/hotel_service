package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.ReservationItem;
import org.example.hotel_service.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {
	@Query("""
			SELECT ri.room.roomId, COUNT(ri.reservationItemId)
			FROM ReservationItem ri
			JOIN ri.reservation r
			WHERE ri.room.roomId IN :roomIds
			  AND r.status IN :statuses
			GROUP BY ri.room.roomId
			""")
	List<Object[]> countBookingsByRoomIds(
			@Param("roomIds") List<Long> roomIds,
			@Param("statuses") List<ReservationStatus> statuses
	);
}

