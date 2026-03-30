package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.ReservationCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationChargeRepository extends JpaRepository<ReservationCharge, Long> {
	List<ReservationCharge> findByReservation_ReservationIdOrderByCreatedAtDesc(Long reservationId);
}

