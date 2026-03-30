package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.RoomType;
import org.example.hotel_service.enums.RoomTypeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
	@Query(value = "SELECT rt FROM RoomType rt LEFT JOIN FETCH rt.hotel",
			countQuery = "SELECT COUNT(rt) FROM RoomType rt")
	Page<RoomType> findAllBy(Pageable pageable);

	@Query(value = "SELECT rt FROM RoomType rt LEFT JOIN FETCH rt.hotel WHERE rt.status = :status",
			countQuery = "SELECT COUNT(rt) FROM RoomType rt WHERE rt.status = :status")
	Page<RoomType> findByStatus(@Param("status") RoomTypeStatus status, Pageable pageable);

	@Query("SELECT rt FROM RoomType rt LEFT JOIN FETCH rt.hotel WHERE rt.roomTypeId = :id")
	Optional<RoomType> findWithDetailsByRoomTypeId(@Param("id") Long roomTypeId);

	boolean existsByCode(String code);

	boolean existsByCodeAndRoomTypeIdNot(String code, Long roomTypeId);
}

