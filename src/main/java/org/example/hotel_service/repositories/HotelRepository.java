package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.Hotel;
import org.example.hotel_service.enums.HotelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

	@EntityGraph(attributePaths = {"images", "roomTypes"})
	Optional<Hotel> findWithImagesAndRoomTypesByHotelId(Integer hotelId);

	@Query("""
			SELECT DISTINCT h FROM Hotel h
			WHERE (:keyword IS NULL
				   OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
				   OR LOWER(h.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:status IS NULL OR h.status = :status)
			  AND (:minStarRating IS NULL OR h.starRating >= :minStarRating)
			  AND (:maxStarRating IS NULL OR h.starRating <= :maxStarRating)
			  AND ((:minPrice IS NULL AND :maxPrice IS NULL) OR EXISTS (
					SELECT 1 FROM RoomType rt
					WHERE rt.hotel = h
					  AND (:minPrice IS NULL OR rt.pricePerNight >= :minPrice)
					  AND (:maxPrice IS NULL OR rt.pricePerNight <= :maxPrice)
			  ))
			""")
	Page<Hotel> searchHotels(@Param("keyword") String keyword,
							 @Param("status") HotelStatus status,
							 @Param("minStarRating") Integer minStarRating,
							 @Param("maxStarRating") Integer maxStarRating,
							 @Param("minPrice") Integer minPrice,
							 @Param("maxPrice") Integer maxPrice,
							 Pageable pageable);
}

