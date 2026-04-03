package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Payment;
import org.example.hotel_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByReservation_ReservationIdOrderByPaidAtDescCreatedAtDesc(Long reservationId);
	Optional<Payment> findByProviderTransId(String providerTransId);
	List<Payment> findByProviderAndStatusAndCreatedAtBefore(String provider, PaymentStatus status, LocalDateTime beforeTime);
}
