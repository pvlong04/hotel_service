package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.ReservationCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationChargeRepository extends JpaRepository<ReservationCharge, Long> {
}

