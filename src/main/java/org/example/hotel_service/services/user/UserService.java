package org.example.hotel_service.services.user;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.hotel_service.dtos.request.CreateUserRequest;
import org.example.hotel_service.dtos.request.UpdateUserRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.UserResponse;
import org.example.hotel_service.entities.Profile;
import org.example.hotel_service.entities.Role;
import org.example.hotel_service.entities.User;
import org.example.hotel_service.entities.UserRole;
import org.example.hotel_service.enums.Roles;
import org.example.hotel_service.enums.UserStatus;
import org.example.hotel_service.exception.ApiException;
import org.example.hotel_service.exception.ErrorCode;
import org.example.hotel_service.mapper.UserMapper;
import org.example.hotel_service.repositories.RoleRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.example.hotel_service.services.notification.NotificationServiceImp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService implements UserServiceImp {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;
    UserMapper mapper;
    NotificationServiceImp notificationService;

    // ─────────────────── helpers ───────────────────

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesObj = jwt.getClaims().get("roles");
        if (rolesObj instanceof Iterable<?> iterable) {
            Set<String> roles = new HashSet<>();
            for (Object item : iterable) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            if (!roles.isEmpty()) {
                return roles;
            }
        }

        Object roleObj = jwt.getClaims().get("role");
        if (roleObj != null) {
            return Set.of(roleObj.toString());
        }
        return Set.of();
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdObj = jwt.getClaims().get("userId");
        if (userIdObj instanceof Number num) return num.longValue();
        return null;
    }

    private boolean hasRole(Jwt jwt, Roles role) {
        return extractRoles(jwt).contains(role.name());
    }

    private Roles resolveActorRole(Jwt jwt) {
        if (hasRole(jwt, Roles.ADMIN)) {
            return Roles.ADMIN;
        }
        if (hasRole(jwt, Roles.STAFF)) {
            return Roles.STAFF;
        }
        return Roles.GUEST;
    }

    private Set<UserRole> buildUserRoles(Set<Roles> requestRoles, User user) {
        if (requestRoles == null || requestRoles.isEmpty()) {
            throw new ApiException(ErrorCode.ROLE_NOT_FOUND);
        }

        Set<UserRole> userRoles = requestRoles.stream()
                .filter(Objects::nonNull)
                .map(roleName -> {
                    Role role = roleRepository.findByName(roleName)
                            .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
                    return UserRole.builder().user(user).role(role).build();
                })
                .collect(Collectors.toSet());

        if (userRoles.isEmpty()) {
            throw new ApiException(ErrorCode.ROLE_NOT_FOUND);
        }
        return userRoles;
    }

    private void validateCreateUserRoles(Set<Roles> requestRoles) {
        if (requestRoles == null || requestRoles.isEmpty()) {
            return;
        }
        if (requestRoles.size() != 1 || !requestRoles.contains(Roles.STAFF)) {
            throw new ApiException(ErrorCode.ILLEGAL_ARGUMENT);
        }
    }

    // ─────────────────── CRUD ───────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(String keyword, UserStatus status, int page, int size, Jwt jwt) {
        if (!hasRole(jwt, Roles.ADMIN) && !hasRole(jwt, Roles.STAFF)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.searchUsers(keyword, status, pageable);

        List<UserResponse> content = userPage.getContent().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId, Jwt jwt) {
        Long requesterId = extractUserId(jwt);

        // GUEST chỉ được xem chính mình
        if (!hasRole(jwt, Roles.ADMIN) && !hasRole(jwt, Roles.STAFF)) {
            if (!userId.equals(requesterId)) {
                throw new ApiException(ErrorCode.ACCESS_DENIED);
            }
        }

        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return mapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Jwt jwt) {
        Long userId = extractUserId(jwt);
        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return mapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, Jwt jwt) {
        if (!hasRole(jwt, Roles.ADMIN)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        validateCreateUserRoles(request.getRoles());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User user = mapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .dob(request.getDob())
                .gender(request.getGender())
                .nationalId(request.getNationalId())
                .nationality(request.getNationality())
                .build();
        user.setProfile(profile);

        user.setUserRoles(buildUserRoles(Set.of(Roles.STAFF), user));

        User saved = userRepository.save(user);
        log.info("ADMIN {} created new user: {}", extractUserId(jwt), saved.getUserId());

        try {
            Long actorId = extractUserId(jwt);
            User actor = actorId != null ? userRepository.findById(actorId).orElse(null) : null;
            if (actor != null) {
                notificationService.notifyHierarchy(
                        actor,
                        Roles.ADMIN,
                        "tao",
                        "tai khoan",
                        saved.getUserId(),
                        "username=" + saved.getUsername()
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to notify hierarchy for createUser {}: {}", saved.getUserId(), ex.getMessage());
        }

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, Jwt jwt) {
        Long requesterId = extractUserId(jwt);
        boolean admin = hasRole(jwt, Roles.ADMIN);

        // STAFF/GUEST chỉ được cập nhật chính mình
        if (!admin && !userId.equals(requesterId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // Cập nhật profile (tất cả role)
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = Profile.builder().user(user).build();
        }
        mapper.updateProfile(request, profile);
        user.setProfile(profile);

        // Các trường chỉ ADMIN được thay đổi
        if (admin) {
            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }
                user.setEmail(request.getEmail());
            }
            if (request.getStatus() != null) {
                user.setStatus(request.getStatus());
            }
            if (request.getRoles() != null && !request.getRoles().isEmpty()) {
                user.getUserRoles().clear();
                user.getUserRoles().addAll(buildUserRoles(request.getRoles(), user));
            }
        }

        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getNationalId() != null) profile.setNationalId(request.getNationalId());
        if (request.getNationality() != null) profile.setNationality(request.getNationality());

        User saved = userRepository.save(user);
        log.info("User {} updated by requester {}", userId, requesterId);

        try {
            User actor = requesterId != null ? userRepository.findById(requesterId).orElse(null) : null;
            if (actor != null) {
                notificationService.notifyHierarchy(
                        actor,
                        resolveActorRole(jwt),
                        "cap nhat",
                        "tai khoan",
                        saved.getUserId(),
                        "username=" + saved.getUsername()
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to notify hierarchy for updateUser {}: {}", saved.getUserId(), ex.getMessage());
        }

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, Jwt jwt) {
        if (!hasRole(jwt, Roles.ADMIN)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        Long requesterId = extractUserId(jwt);
        if (userId.equals(requesterId)) {
            throw new ApiException(ErrorCode.CANNOT_DELETE_SELF);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        User actor = requesterId != null ? userRepository.findById(requesterId).orElse(null) : null;

        userRepository.delete(user);
        log.info("ADMIN {} deleted user {}", requesterId, userId);

        try {
            if (actor != null) {
                notificationService.notifyHierarchy(
                        actor,
                        Roles.ADMIN,
                        "xoa",
                        "tai khoan",
                        userId,
                        "username=" + user.getUsername()
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to notify hierarchy for deleteUser {}: {}", userId, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatus status, Jwt jwt) {
        if (!hasRole(jwt, Roles.ADMIN)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(status);
        User saved = userRepository.save(user);
        log.info("ADMIN {} changed status of user {} to {}", extractUserId(jwt), userId, status);
        return mapper.toResponse(saved);
    }
}
