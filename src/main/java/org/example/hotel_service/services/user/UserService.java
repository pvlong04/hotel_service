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
import org.example.hotel_service.repositories.RoleRepository;
import org.example.hotel_service.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService implements UserServiceImp {

    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    // ─────────────────── helpers ───────────────────

    private String extractRole(Jwt jwt) {
        Object roleObj = jwt.getClaims().get("role");
        return roleObj != null ? roleObj.toString() : "";
    }

    private Long extractUserId(Jwt jwt) {
        Object userIdObj = jwt.getClaims().get("userId");
        if (userIdObj instanceof Number num) return num.longValue();
        return null;
    }

    private boolean isAdmin(Jwt jwt) {
        return Roles.ADMIN.name().equals(extractRole(jwt));
    }

    private boolean isStaff(Jwt jwt) {
        return Roles.STAFF.name().equals(extractRole(jwt));
    }

    private UserResponse toResponse(User user) {
        List<String> roles = user.getUserRoles() == null ? List.of()
                : user.getUserRoles().stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getName().name())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .fullName(user.getProfile() != null ? user.getProfile().getFullName() : null)
                .phone(user.getProfile() != null ? user.getProfile().getPhone() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .address(user.getProfile() != null ? user.getProfile().getAddress() : null)
                .dob(user.getProfile() != null ? user.getProfile().getDob() : null)
                .roles(roles)
                .build();
    }

    // ─────────────────── CRUD ───────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(String keyword, UserStatus status, int page, int size, Jwt jwt) {
        if (!isAdmin(jwt) && !isStaff(jwt)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.searchUsers(keyword, status, pageable);

        List<UserResponse> content = userPage.getContent().stream()
                .map(this::toResponse)
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
        if (!isAdmin(jwt) && !isStaff(jwt)) {
            if (!userId.equals(requesterId)) {
                throw new ApiException(ErrorCode.ACCESS_DENIED);
            }
        }

        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Jwt jwt) {
        Long userId = extractUserId(jwt);
        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseGet(() -> roleRepository.save(Role.builder().name(request.getRole()).build()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .build();

        Profile profile = Profile.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .dob(request.getDob())
                .build();
        user.setProfile(profile);

        UserRole userRole = UserRole.builder().user(user).role(role).build();
        user.setUserRoles(List.of(userRole));

        User saved = userRepository.save(user);
        log.info("ADMIN {} created new user: {}", extractUserId(jwt), saved.getUserId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request, Jwt jwt) {
        Long requesterId = extractUserId(jwt);
        boolean admin = isAdmin(jwt);

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
        if (request.getFullName() != null) profile.setFullName(request.getFullName());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getDob() != null) profile.setDob(request.getDob());
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
//            if (request.getRole() != null) {
//                Role newRole = roleRepository.findByName(request.getRole())
//                        .orElseGet(() -> roleRepository.save(Role.builder().name(request.getRole()).build()));
//                // Xóa role cũ, gán role mới
//                user.getUserRoles().clear();
//                UserRole userRole = UserRole.builder().user(user).role(newRole).build();
//                user.getUserRoles().add(userRole);
//            }
        }

        User saved = userRepository.save(user);
        log.info("User {} updated by requester {}", userId, requesterId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId, Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        Long requesterId = extractUserId(jwt);
        if (userId.equals(requesterId)) {
            throw new ApiException(ErrorCode.CANNOT_DELETE_SELF);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
        log.info("ADMIN {} deleted user {}", requesterId, userId);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatus status, Jwt jwt) {
        if (!isAdmin(jwt)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        User user = userRepository.findWithProfileAndRolesByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        user.setStatus(status);
        User saved = userRepository.save(user);
        log.info("ADMIN {} changed status of user {} to {}", extractUserId(jwt), userId, status);
        return toResponse(saved);
    }
}
