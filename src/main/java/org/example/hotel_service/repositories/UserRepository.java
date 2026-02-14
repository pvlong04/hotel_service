package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    //    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String username, String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"profile", "userRoles", "userRoles.role"})
    Optional<User> findWithProfileAndRolesByUsernameOrEmail(String username, String email);
}
