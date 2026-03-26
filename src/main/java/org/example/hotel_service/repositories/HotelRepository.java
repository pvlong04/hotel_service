package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Hotel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

	@EntityGraph(attributePaths = {"images", "roomTypes"})
	Optional<Hotel> findFirstWithImagesAndRoomTypesByOrderByHotelIdAsc();

	Optional<Hotel> findFirstByOrderByHotelIdAsc();
}

