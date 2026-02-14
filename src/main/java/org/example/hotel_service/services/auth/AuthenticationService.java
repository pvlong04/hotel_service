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
import org.example.hotel_service.dtos.request.RegisterRequest;
import org.example.hotel_service.dtos.response.AuthResponse;
import org.example.hotel_service.entities.Profile;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.mapper.UserMapper;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService implements AuthenticationServiceImp {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    org.example.hotel_service.repositories.RoleRepository roleRepository;

    JwtProperties jwtProperties;

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

//        User savedUser = userRepository.save(user);

//        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
//                .userId(savedUser.getUserId())
//                .email(savedUser.getEmail())
//                .fullName(savedUser.getProfile() != null ? savedUser.getProfile().getFullName() : null)
//                .avatarUrl(savedUser.getProfile() != null ? savedUser.getProfile().getAvatarUrl() : null)
//                .role(savedUser.getUserRoles() != null && !savedUser.getUserRoles().isEmpty()
//                        && savedUser.getUserRoles().get(0).getRole() != null
//                        ? String.valueOf(savedUser.getUserRoles().get(0).getRole().getName())
//                        : null)
//                .build();
//
        return AuthResponse.builder()
//                .accessToken(null)
//                .refreshToken(null)
//                .tokenType(null)
//                .expiresIn(null)
//                .user(userInfo)
                .build();
    }


    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsernameOrEmail();
        User user = userRepository.findWithProfileAndRolesByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_EXIT));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        String accessToken = generateToken(user.getUsername());

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
                .refreshToken(null)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(userInfo)
                .build();
    }

    private String generateToken(String username) {

        JWSHeader jweHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("pvlong04")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .claim("Custom", "Custom")
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
}
