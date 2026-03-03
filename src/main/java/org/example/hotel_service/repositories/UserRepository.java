package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.User;
import org.example.hotel_service.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    @EntityGraph(attributePaths = {"profile", "userRoles", "userRoles.role"})
    Optional<User> findWithProfileAndRolesByUsernameOrEmail(String username, String email);

    @EntityGraph(attributePaths = {"profile", "userRoles", "userRoles.role"})
    Optional<User> findWithProfileAndRolesByUserId(Long userId);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN u.profile p
            WHERE (:keyword IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("status") UserStatus status,
                           Pageable pageable);
}



