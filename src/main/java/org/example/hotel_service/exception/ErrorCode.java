package org.example.hotel_service.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNCATEGORIZED_EXIT(9999, "Lỗi hệ thống"),
    USER_EXIT_EMAIL(1001, "Email người dùng đã tồn tại"),
    USER_NOT_FOUND(1002, "Không tìm thấy người dùng"),
    USERNAME_VALID(1003, "Tên người dùng ít nhất là 3 ký tự"),
    PASSWORD_VALID(1004, "Mật khẩu ít nhất là 8 ký tự"),
    KEY_VALID(1005, "Lỗi hệ thống"),
    USER_NOT_EXIT(1006, "Người dùng không tồn tại"),
    UNAUTHENTICATED(1007, "Đăng nhập thất bại"),
    USER_EXITS(1008, "Người dùng đã tồn tại"),
    INVALID_REFRESH_TOKEN(1009, "Refresh token không hợp lệ hoặc đã bị thu hồi"),
    REFRESH_TOKEN_EXPIRED(1010, "Refresh token đã hết hạn"),
    ROLE_NOT_FOUND(1011, "Vai trò không tồn tại"),
    ACCESS_DENIED(1012, "Bạn không có quyền thực hiện thao tác này"),
    USER_ALREADY_BANNED(1013, "Tài khoản đã bị khóa"),
    CANNOT_DELETE_SELF(1014, "Không thể xóa tài khoản của chính mình"),
    USERNAME_ALREADY_EXISTS(1015, "Tên đăng nhập đã tồn tại"),
    EMAIL_ALREADY_EXISTS(1016, "Email đã tồn tại"),
    HOTEL_NOT_FOUND(1017, "Không tìm thấy khách sạn"),
    ROOM_TYPE_NOT_FOUND(1018, "Không tìm thấy loại phòng"),
    FLOOR_NOT_FOUND(1019, "Không tìm thấy tầng"),
    ROOM_NOT_FOUND(1020, "Không tìm thấy phòng"),
    ROOM_ALREADY_EXISTS(1021, "Số phòng đã tồn tại trong khách sạn"),
    INVALID_DATE_RANGE(1022, "Khoảng ngày không hợp lệ"),
    RESERVATION_NOT_FOUND(1023, "Không tìm thấy đơn đặt phòng"),
    RESERVATION_ACCESS_DENIED(1024, "Bạn không có quyền truy cập đơn đặt phòng này"),
    RESERVATION_STATUS_TRANSITION_INVALID(1025, "Chuyển trạng thái đơn đặt phòng không hợp lệ"),
    PAYMENT_AMOUNT_INVALID(1026, "Số tiền thanh toán không hợp lệ"),
    OPERATION_NOT_ALLOWED(1027, "Thao tác không được phép trong chế độ 1 khách sạn"),
    ILLEGAL_ARGUMENT(1028, "Đối số không hợp lệ"),
    INVALID_DATA_ACCESS(1029, "Lỗi truy cập dữ liệu không hợp lệ"),
    USER_INACTIVE_BANE(1030, "Tài khoản chưa được kích hoạt hoặc đã bị khóa"),
    PAYMENT_NOT_FOUND(1031, "Không tìm thấy giao dịch thanh toán"),
    PAYMENT_ALREADY_COMPLETED(1032, "Giao dịch thanh toán đã hoàn thành"),
    AMENITY_NOT_FOUND(1033, "Không tìm thấy tiện nghi"),
    EMAIL_VERIFICATION_TOKEN_INVALID(1034, "Token xac thuc email khong hop le hoac da het han"),
    EMAIL_ALREADY_VERIFIED(1035, "Tai khoan da duoc kich hoat truoc do"),
    RUNTIME_ERROR(1036, "Lỗi thực thi"),
    INVALID_REQUEST(1037, "File rỗng hoặc không hợp lệ"),;
    int code;
    String message;
}
