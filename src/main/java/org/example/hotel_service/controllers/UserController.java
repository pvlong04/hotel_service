package org.example.hotel_service.controllers;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.hotel_service.api.ApiResponse;
import org.example.hotel_service.dtos.request.CreateUserRequest;
import org.example.hotel_service.dtos.request.UpdateUserRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.UserResponse;
import org.example.hotel_service.enums.UserStatus;
import org.example.hotel_service.services.user.UserServiceImp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý người dùng
 *
 * Phân quyền:
 *  - ADMIN : full CRUD, thay đổi role/status
 *  - STAFF : xem danh sách & chi tiết; cập nhật profile của chính mình
 *  - GUEST : xem & cập nhật profile của chính mình
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserServiceImp userService;

    /**
     * GET /users
     * Lấy danh sách người dùng (ADMIN, STAFF)
     *
     * @param keyword   tìm kiếm theo username / email / fullName
     * @param status    lọc theo trạng thái tài khoản
     * @param page      số trang (mặc định 0)
     * @param size      kích thước trang (mặc định 10)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {

        PageResponse<UserResponse> result = userService.getAllUsers(keyword, status, page, size, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", result));
    }

    /**
     * GET /users/me
     * Lấy thông tin người dùng hiện tại (tất cả role)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal Jwt jwt) {

        UserResponse result = userService.getMyProfile(jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", result));
    }

    /**
     * GET /users/{userId}
     * Lấy thông tin người dùng theo ID
     *  - ADMIN, STAFF: xem bất kỳ ai
     *  - GUEST        : chỉ xem chính mình
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {

        UserResponse result = userService.getUserById(userId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", result));
    }

    /**
     * POST /users
     * Tạo người dùng mới (chỉ ADMIN)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UserResponse result = userService.createUser(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo người dùng thành công", result));
    }

    /**
     * PUT /users/{userId}
     * Cập nhật thông tin người dùng
     *  - ADMIN : cập nhật tất cả trường (email, role, status, profile)
     *  - STAFF / GUEST : chỉ cập nhật profile của chính mình
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UserResponse result = userService.updateUser(userId, request, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", result));
    }

    /**
     * DELETE /users/{userId}
     * Xóa người dùng (chỉ ADMIN, không thể tự xóa chính mình)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt) {

        userService.deleteUser(userId, jwt);
        return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", null));
    }

    /**
     * PATCH /users/{userId}/status
     * Khóa / mở khóa tài khoản (chỉ ADMIN)
     *
     * @param status  ACTIVE | BANNED | PENDING
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam UserStatus status,
            @AuthenticationPrincipal Jwt jwt) {

        UserResponse result = userService.updateUserStatus(userId, status, jwt);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tài khoản thành công", result));
    }
}


