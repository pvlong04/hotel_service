package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Integer> {
}

