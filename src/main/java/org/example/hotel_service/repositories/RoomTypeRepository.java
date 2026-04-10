package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.enums.RoomTypeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

	/**
	 * Paginated queries: Không dùng @EntityGraph vì Hibernate không thể
	 * fetch nhiều bag-collection (amenities + images) cùng lúc với pagination
	 * → InvalidDataAccessApiUsageException. Lazy loading xử lý bởi @Transactional.
	 */
	@Query(
			value = """
					SELECT rt FROM RoomType rt
					WHERE (:status IS NULL OR rt.status = :status)
					  AND (:priceMin IS NULL OR rt.pricePerNight >= :priceMin)
					  AND (:priceMax IS NULL OR rt.pricePerNight <= :priceMax)
					""",
			countQuery = """
					SELECT COUNT(rt) FROM RoomType rt
					WHERE (:status IS NULL OR rt.status = :status)
					  AND (:priceMin IS NULL OR rt.pricePerNight >= :priceMin)
					  AND (:priceMax IS NULL OR rt.pricePerNight <= :priceMax)
					"""
	)
	Page<RoomType> findByFilters(
			@Param("status") RoomTypeStatus status,
			@Param("priceMin") Long priceMin,
			@Param("priceMax") Long priceMax,
			Pageable pageable
	);

	@Query("SELECT rt FROM RoomType rt WHERE rt.roomTypeId = :id")
	Optional<RoomType> findWithDetailsByRoomTypeId(@Param("id") Long roomTypeId);

	boolean existsByCode(String code);

	boolean existsByCodeAndRoomTypeIdNot(String code, Long roomTypeId);
}

