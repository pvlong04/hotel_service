package org.example.hotel_service.repositories;

import org.example.hotel_service.entities.AuthToken;
import org.example.hotel_service.enums.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    void deleteByUser_UserIdAndPurposeAndUsedAtIsNull(Long userId, TokenPurpose purpose);

    Optional<AuthToken> findTopByUser_UserIdAndPurposeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            TokenPurpose purpose,
            LocalDateTime now
    );
}

