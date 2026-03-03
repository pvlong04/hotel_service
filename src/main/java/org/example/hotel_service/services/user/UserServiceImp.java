package org.example.hotel_service.services.user;

import org.example.hotel_service.dtos.request.CreateUserRequest;
import org.example.hotel_service.dtos.request.UpdateUserRequest;
import org.example.hotel_service.dtos.response.PageResponse;
import org.example.hotel_service.dtos.response.UserResponse;
import org.example.hotel_service.enums.UserStatus;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserServiceImp {

    /** Lấy danh sách người dùng có phân trang + tìm kiếm (ADMIN, STAFF) */
    PageResponse<UserResponse> getAllUsers(String keyword, UserStatus status, int page, int size, Jwt jwt);

    /** Lấy thông tin người dùng theo ID (ADMIN, STAFF xem tất cả; GUEST chỉ xem chính mình) */
    UserResponse getUserById(Long userId, Jwt jwt);

    /** Lấy thông tin người dùng hiện tại (tất cả role) */
    UserResponse getMyProfile(Jwt jwt);

    /** Tạo người dùng mới (chỉ ADMIN) */
    UserResponse createUser(CreateUserRequest request, Jwt jwt);

    /** Cập nhật thông tin người dùng (ADMIN: full; STAFF/GUEST: chỉ chính mình + profile) */
    UserResponse updateUser(Long userId, UpdateUserRequest request, Jwt jwt);

    /** Xóa người dùng (chỉ ADMIN) */
    void deleteUser(Long userId, Jwt jwt);

    /** Khóa/mở khóa tài khoản (chỉ ADMIN) */
    UserResponse updateUserStatus(Long userId, UserStatus status, Jwt jwt);
}
