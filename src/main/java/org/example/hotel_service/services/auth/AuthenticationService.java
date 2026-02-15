package org.example.hotel_service.services.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.config.JwtProperties;
import org.example.hotel_service.dtos.request.LoginRequest;
import org.example.hotel_service.dtos.request.RefreshTokenRequest;
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.entities.Profile;
import org.example.hotel_service.entities.RefreshToken;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.mapper.UserMapper;
import org.example.hotel_service.repositories.RefreshTokenRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService implements AuthenticationServiceImp {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    org.example.hotel_service.repositories.RoleRepository roleRepository;
    RefreshTokenRepository refreshTokenRepository;

    JwtProperties jwtProperties;

    @org.springframework.transaction.annotation.Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.USER_EXIT_EMAIL);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.USER_EXITS);
        }

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Role guestRole = roleRepository.findByName(org.example.hotel_service.enums.Roles.GUEST)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Roles.GUEST).build()));

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();
        user.setProfile(profile);

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(guestRole)
                .build();
        user.setUserRoles(java.util.List.of(userRole));

        User savedUser = userRepository.save(user);

        String accessToken = generateAccessToken(savedUser);
        String refreshToken = issueRefreshToken(savedUser, null, null);

        return getAuthResponse(savedUser, accessToken, refreshToken);
    }


    @org.springframework.transaction.annotation.Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String identifier = request.getUsernameOrEmail();
        User user = userRepository.findWithProfileAndRolesByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_EXIT));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        user.setLastLoginAt(LocalDateTime.now());

        return getAuthResponse(userAgent, ipAddress, user);
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String userAgent, String ipAddress) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getRevokedAt() != null) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        storedToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        return getAuthResponse(userAgent, ipAddress, user);
    }

    private AuthResponse getAuthResponse(String userAgent, String ipAddress, User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = issueRefreshToken(user, userAgent, ipAddress);

        return getAuthResponse(user, accessToken, refreshToken);
    }

    private AuthResponse getAuthResponse(User user, String accessToken, String refreshToken) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .role(user.getUserRoles() != null && !user.getUserRoles().isEmpty()
                        && user.getUserRoles().get(0).getRole() != null
                        ? String.valueOf(user.getUserRoles().get(0).getRole().getName())
                        : null)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenMinutes().longValue() * 60)
                .user(userInfo)
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getRevokedAt() == null) {
            storedToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(storedToken);
        }
    }

    private String generateAccessToken(User user) {
        JWSHeader jweHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("pvlong04")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(jwtProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES).toEpochMilli()))
                .claim("nonce", generateTokenNonce())
                .claim("userId", user.getUserId())
                .claim("role", user.getUserRoles() != null && !user.getUserRoles().isEmpty()
                        && user.getUserRoles().get(0).getRole() != null
                        ? String.valueOf(user.getUserRoles().get(0).getRole().getName())
                        : null)
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jweHeader, payload);

        try {
            jwsObject.sign(new MACSigner(jwtProperties.getSignerKey().getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private String issueRefreshToken(User user, String userAgent, String ipAddress) {
        String rawToken = generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenDays()))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String generateTokenNonce() {
        byte[] randomBytes = new byte[16];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
