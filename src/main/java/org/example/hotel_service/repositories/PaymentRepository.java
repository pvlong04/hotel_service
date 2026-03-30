package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByReservation_ReservationIdOrderByPaidAtDescCreatedAtDesc(Long reservationId);
}

