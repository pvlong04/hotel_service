package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Amenity;
import org.example.hotel_service.enums.AmenityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Integer> {
    List<Amenity> findByCategory(AmenityCategory category);
}

