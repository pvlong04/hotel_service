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
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.config.JwtProperties;
import org.example.hotel_service.dtos.request.LoginRequest;
import org.example.hotel_service.dtos.request.RefreshTokenRequest;
import org.example.hotel_service.dtos.request.ResendVerificationRequest;
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.entities.AuthToken;
import org.example.hotel_service.entities.Profile;
import org.example.hotel_service.entities.RefreshToken;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.TokenPurpose;
import org.example.hotel_service.enums.UserStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.mapper.UserMapper;
import org.example.hotel_service.repositories.AuthTokenRepository;
import org.example.hotel_service.repositories.RefreshTokenRepository;
import org.example.hotel_service.repositories.RoleRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.example.hotel_service.services.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService implements AuthenticationServiceImp {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;
    RefreshTokenRepository refreshTokenRepository;
    AuthTokenRepository authTokenRepository;
    EmailService emailService;

    JwtProperties jwtProperties;

    @NonFinal
    @Value("${app.mail.verify-base-url:http://localhost:9000/auth/verify-email}")
    String verifyBaseUrl;

    @NonFinal
    @Value("${app.mail.verify-token-ttl-hours:24}")
    long verifyTokenTtlHours;

    @NonFinal
    @Value("${app.mail.verify-resend-cooldown-seconds:60}")
    long resendCooldownSeconds;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.USER_EXIT_EMAIL);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.USER_EXITS);
        }

        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING);

        Role guestRole = roleRepository.findByName(Roles.GUEST)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(Roles.GUEST)
                        .build()));

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

//        Set<UserRole> userRoles = new HashSet<>();
//        userRoles.add(userRole);
//        user.setUserRoles(userRoles);
        user.setUserRoles(Set.of(userRole));
        User savedUser = userRepository.save(user);

        issueAndSendVerificationToken(savedUser);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .userId(savedUser.getUserId())
                        .username(savedUser.getUsername())
                        .email(savedUser.getEmail())
                        .fullName(savedUser.getProfile() != null ? savedUser.getProfile().getFullName() : null)
                        .avatarUrl(savedUser.getProfile() != null ? savedUser.getProfile().getAvatarUrl() : null)
                        .roles(extractRoleNames(savedUser))
                        .build())
                .build();
    }


    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String identifier = request.getUsernameOrEmail();
        User user = userRepository.findWithProfileAndRolesByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_EXIT));

//        if (user.getStatus() == UserStatus.PENDING) {
//            throw new ApiException(ErrorCode.USER_INACTIVE);
//        }
//
//        if (user.getStatus() == UserStatus.BANNED) {
//            throw new ApiException(ErrorCode.USER_ALREADY_BANNED);
//        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(ErrorCode.USER_INACTIVE_BANE);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        user.setLastLoginAt(LocalDateTime.now());

        return getAuthResponse(userAgent, ipAddress, user);
    }

    @Transactional
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
        if (user.getStatus() == UserStatus.BANNED) {
            throw new ApiException(ErrorCode.   USER_ALREADY_BANNED);
        }
        return getAuthResponse(userAgent, ipAddress, user);
    }

    private AuthResponse getAuthResponse(String userAgent, String ipAddress, User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = issueRefreshToken(user, userAgent, ipAddress);

        return getAuthResponse(user, accessToken, refreshToken);
    }

    private AuthResponse getAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roles = extractRoleNames(user);
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .roles(roles)
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenMinutes().longValue() * 60)
                .user(userInfo)
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getRevokedAt() == null) {
            storedToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(storedToken);
        }
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
        }

        AuthToken authToken = authTokenRepository.findByTokenHashAndPurpose(hashToken(token), TokenPurpose.VERIFY_EMAIL)
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID));

        User user = authToken.getUser();
        LocalDateTime now = LocalDateTime.now();

        // Idempotent behavior: if this token was already consumed and user is ACTIVE,
        // treat subsequent calls as success (common with repeated clicks/dev strict mode).
        if (authToken.getUsedAt() != null) {
            if (user.getStatus() == UserStatus.ACTIVE) {
                return;
            }
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
        }

        if (authToken.getExpiresAt().isBefore(now)) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            authToken.setUsedAt(now);
            authTokenRepository.save(authToken);
            return;
        }

        authToken.setUsedAt(now);
        user.setStatus(UserStatus.ACTIVE);
        authTokenRepository.save(authToken);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    if (user.getStatus() == UserStatus.PENDING) {
                        LocalDateTime now = LocalDateTime.now();
                        boolean inCooldown = authTokenRepository
                                .findTopByUser_UserIdAndPurposeAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                                        user.getUserId(), TokenPurpose.VERIFY_EMAIL, now)
                                .map(existingToken -> existingToken.getCreatedAt() != null
                                        && existingToken.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(now))
                                .orElse(false);

                        if (inCooldown) {
                            return;
                        }

                        issueAndSendVerificationToken(user);
                    }
                });
    }

    private String generateAccessToken(User user) {
        JWSHeader jweHeader = new JWSHeader(JWSAlgorithm.HS512);
        Set<String> roles = extractRoleNames(user);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("pvlong04")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(jwtProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES).toEpochMilli()))
                .claim("nonce", generateTokenNonce())
                .claim("userId", user.getUserId())
                .claim("roles", roles)
                // Keep legacy single role claim for backward compatibility.
                .claim("role", resolvePrimaryRole(roles))
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

    private void issueAndSendVerificationToken(User user) {
        String rawToken = generateRefreshTokenValue();
        authTokenRepository.deleteByUser_UserIdAndPurposeAndUsedAtIsNull(user.getUserId(), TokenPurpose.VERIFY_EMAIL);

        AuthToken authToken = AuthToken.builder()
                .user(user)
                .purpose(TokenPurpose.VERIFY_EMAIL)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(verifyTokenTtlHours))
                .build();
        authTokenRepository.save(authToken);

        String verifyLink = buildVerifyLink(rawToken);
        emailService.sendVerificationEmail(user, verifyLink);
    }

    private String buildVerifyLink(String rawToken) {
        String baseUrl = verifyBaseUrl == null ? "" : verifyBaseUrl.trim();
        return UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("token", rawToken)
                .build()
                .toUriString();
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

    private Set<String> extractRoleNames(User user) {
        if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            return Set.of();
        }
        return user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                .map(userRole -> userRole.getRole().getName().name())
                .collect(Collectors.toSet());
    }

    private String resolvePrimaryRole(Set<String> roles) {
        return roles.stream()
                .min(Comparator.comparingInt(this::rolePriority))
                .orElse(null);
    }

    private int rolePriority(String role) {
        if (Roles.ADMIN.name().equals(role)) return 1;
        if (Roles.STAFF.name().equals(role)) return 2;
        if (Roles.GUEST.name().equals(role)) return 3;
        return Integer.MAX_VALUE;
    }
}
