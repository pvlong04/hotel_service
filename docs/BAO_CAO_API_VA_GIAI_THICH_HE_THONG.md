# Báo cáo API còn thiếu + API cần cải tiến + giải thích hệ thống (hotel_service)

Ngày cập nhật: 2026-03-25  
Dự án: `hotel_service`

## Checklist nội dung tài liệu
- [x] Liệt kê API hiện có (theo Controller)
- [x] Liệt kê API còn thiếu (theo Entity/DTO/Repository đã có)
- [x] Liệt kê API đã có nhưng chưa hoàn thiện + đề xuất cải tiến
- [x] Giải thích code theo luồng + giải thích các annotation/keyword/khái niệm sử dụng trong từng dòng

---

## 1) Danh sách API hiện có (đang dùng được)

> Nguồn: thư mục `src/main/java/org/example/hotel_service/controllers/*Controller.java`

### 1.1 Auth (`/auth`)
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### 1.2 Users (`/users`)
- `GET /users` (ADMIN/STAFF)
- `GET /users/me`
- `GET /users/{userId}`
- `POST /users` (ADMIN)
- `PUT /users/{userId}`
- `DELETE /users/{userId}` (ADMIN)
- `PATCH /users/{userId}/status` (ADMIN)

### 1.3 Hotels (`/hotels`)
- `POST /hotels/search`
- `GET /hotels/{hotelId}`
- `POST /hotels` (ADMIN)
- `PUT /hotels/{hotelId}` (ADMIN)
- `DELETE /hotels/{hotelId}` (ADMIN)

### 1.4 Rooms (`/rooms`)
- `GET /rooms`
- `GET /rooms/{id}`
- `POST /rooms` (ADMIN)
- `PUT /rooms/{id}` (ADMIN/STAFF)
- `DELETE /rooms/{id}` (ADMIN)
- `GET /rooms/available?hotelId=&checkIn=&checkOut=`
- `GET /rooms/{id}/images`

### 1.5 Reservations (`/reservations`)
- `POST /reservations/availability`
- `POST /reservations`
- `GET /reservations/{reservationId}`
- `GET /reservations/me`
- `GET /reservations?hotelId=&status=` (STAFF/ADMIN)
- `PATCH /reservations/{reservationId}/status`
- `POST /reservations/payments`
- `POST /reservations/charges`

---

## 2) Các API còn thiếu của hệ thống (ưu tiên theo mức độ)

> Cách xác định “còn thiếu”: trong codebase đã có `Entity/Repository/DTO` hoặc có file SQL tạo bảng nhưng **chưa có Controller/Service** tương ứng.

### P0 (cần để hệ thống vận hành trơn tru)

#### 2.1 RoomType Management (thiếu Controller)
Đã có: `RoomType`, `RoomTypeRepository`, ảnh `RoomTypeImage`, amenity mapping.  
Thiếu đề xuất API:
- `GET /room-types?hotelId=` (list)
- `GET /room-types/{id}` (detail)
- `POST /room-types` (ADMIN)
- `PUT /room-types/{id}` (ADMIN)
- `DELETE /room-types/{id}` (ADMIN)
- `POST /room-types/{id}/images` (ADMIN)
- `DELETE /room-types/{id}/images/{imageId}` (ADMIN)
- (tuỳ scope) `PUT /room-types/{id}/amenities` (ADMIN)

#### 2.2 Floor Management (thiếu Controller)
Đã có: `Floor`, `FloorRepository`.  
Thiếu đề xuất API:
- `GET /floors?hotelId=`
- `GET /floors/{id}`
- `POST /floors` (ADMIN)
- `PUT /floors/{id}` (ADMIN)
- `DELETE /floors/{id}` (ADMIN)

#### 2.3 Reservation History APIs (thiếu endpoint xem lịch sử)
Đã có: entities `Payment`, `ReservationCharge` và quan hệ `Reservation.payments/charges`.  
Thiếu đề xuất API:
- `GET /reservations/{id}/payments`
- `GET /reservations/{id}/charges`

### P1 (nâng trải nghiệm, hoàn thiện nghiệp vụ)

#### 2.4 Notifications
Đã có entity `Notification` và bảng trong SQL migration.  
Thiếu đề xuất API:
- `GET /notifications/me`
- `PATCH /notifications/{id}/read`
- `PATCH /notifications/read-all`

#### 2.5 Password self-service (quên mật khẩu/OTP/đổi mật khẩu)
Trong DTO folder có các request như `ForgotPasswordRequest`, `VerifyOtpRequest`, `ResetPasswordRequest`, `ChangePasswordRequest`.
Thiếu đề xuất API:
- `POST /auth/forgot-password`
- `POST /auth/verify-otp`
- `POST /auth/reset-password`
- `POST /users/me/change-password`

#### 2.6 Reviews
Trong DTO có `CreateReviewRequest`, `ReplyReviewRequest`.
Thiếu đề xuất API:
- `POST /reviews`
- `GET /hotels/{hotelId}/reviews`
- `POST /reviews/{reviewId}/reply`

### P2 (báo cáo, quản trị)

#### 2.7 Dashboard/Stats
Có DTO `DashboardStatsResponse`.
Thiếu đề xuất API:
- `GET /dashboard/stats?hotelId=&from=&to=`

---

## 3) API đã có nhưng chưa hoàn thiện/cần cải tiến

### 3.1 Hotel search theo date-range
Hiện đã cải tiến: `HotelService.searchHotels()` có tính `availableRooms` theo date-range.
Cần cải tiến thêm:
- Tránh N+1: hiện đang lặp từng hotel để gọi `roomRepository.findAvailableRooms(...)`.
  - Giải pháp: thêm query aggregate theo hotelId (GROUP BY) để trả về số phòng trống theo danh sách hotelIds.
- Thêm filter theo `capacity` (adults+children) và `roomType` (nếu cần nâng cấp search).

### 3.2 Reservation flow
Đã có create/status/payment/charge.
Cần cải tiến thêm:
- Refund API + Payment status transitions (PENDING/FAILED/REFUNDED)
- Idempotency key cho payment online
- Booking policy: late cancel fee, no-show
- Transaction boundaries rõ ràng hơn nếu triển khai external payment gateway

### 3.3 Auth response format
Đã chuẩn hoá theo `ApiResponse.success(...)`.
Cần cải tiến thêm:
- Thống nhất `code` (0/200) và `success=true/false` cho **tất cả** controller
- Đảm bảo `timestamp` luôn có

### 3.4 Security hardening
Đã cải tiến:
- Secrets chuyển sang env var trong `application.properties`
- Bootstrap admin bị disable mặc định
Cần cải tiến thêm:
- CORS policy, rate limit login
- Profiles `dev/staging/prod`
- Ẩn/mask secrets khi log

### 3.5 Test coverage
Đã có:
- `HotelServiceApplicationTests` (context)
- `ReservationServiceTest` (unit test bằng Mockito)
Cần cải tiến thêm:
- Integration test cho controller (MockMvc)
- Regression test theo Postman/collection

---

## 4) Giải thích luồng hoạt động hệ thống (end-to-end)

### 4.1 Luồng đăng nhập
1. `POST /auth/login` -> `AuthenticationService.login()` xác thực username/email + password
2. Trả về JWT access token (claims: `userId`, `roles`) + refresh token
3. Client gửi `Authorization: Bearer <accessToken>` cho mọi request private

### 4.2 Luồng đặt phòng (Reservation)
1. `POST /reservations/availability` -> lấy danh sách phòng trống
2. `POST /reservations` -> tạo `Reservation` + `ReservationItem` (snapshot giá)
3. `PATCH /reservations/{id}/status`:
   - STAFF/ADMIN: CONFIRMED -> CHECKED_IN -> CHECKED_OUT
   - GUEST: chỉ được CANCELLED khi PENDING/CONFIRMED
4. `POST /reservations/payments`: ghi nhận thanh toán (Update `paidAmount`)
5. `POST /reservations/charges`: thêm phụ phí (Update `totalAmount`)

---

## 5) Giải thích code: cách đọc “từng dòng” và những thứ dùng trong dòng code

> Bạn yêu cầu “giải thích từng dòng” toàn bộ dự án. Dự án hiện có >100 file Java, nếu ghi tất cả vào 1 file sẽ cực dài.
> Vì vậy tài liệu này:
> - Giải thích **cực chi tiết theo mẫu** (các annotation, keyword, concept) để bạn tự áp dụng cho mọi file.
> - Giải thích **chi tiết các file core** (controllers/services/config/exception) vì đây là nơi chứa luồng nghiệp vụ.

### 5.1 Nhóm annotation Spring / Jakarta Validation
- `@RestController`: biến class thành REST controller. Mỗi method trả về object -> Spring serialize thành JSON.
- `@RequestMapping("/path")`: tiền tố URL cho toàn bộ method trong class.
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`: map HTTP method.
- `@RequestBody`: lấy JSON body và map vào DTO.
- `@RequestParam`: query string.
- `@PathVariable`: lấy biến trên URL.
- `@Valid`: kích hoạt validation trên DTO.
- `@NotNull`, `@Min`, `@Size`, ...: rule validate.

### 5.2 Spring Security JWT
- `@AuthenticationPrincipal Jwt jwt`: Spring đưa JWT đã decode vào method.
- `jwt.getClaims()`: đọc claim (`userId`, `roles`).

### 5.3 JPA/Hibernate
- `@Entity`: class map bảng DB.
- `@Table(name="...")`: chỉ định bảng.
- `@Id`, `@GeneratedValue`: primary key.
- `@ManyToOne`, `@OneToMany`: quan hệ.
- `@EntityGraph`: tối ưu fetch, tránh N+1.

### 5.4 Lombok
- `@RequiredArgsConstructor`: tạo constructor inject các field `final`.
- `@FieldDefaults(level=..., makeFinal=true)`: giảm boilerplate.
- `@Builder`: builder pattern.
- `@Data/@Getter/@Setter`: generate getter/setter.

---

## 6) Giải thích chi tiết (theo file core)

### 6.1 `SecurityConfig.java`
- Mục tiêu: chỉ public auth endpoints, còn lại phải có token.
- JWT decoder dùng HS512 với secret key.

### 6.2 `ApplicationInitConfig.java`
- Mục tiêu: bootstrap admin user.
- Đã cải tiến: chỉ seed khi `app.bootstrap.admin.enabled=true` và password đủ mạnh.

### 6.3 `GlobalExceptionHandle.java`
- Bắt exception và trả về `ApiResponse` chuẩn.
- `MethodArgumentNotValidException` trả về map field->message.

### 6.4 `ReservationController.java` + `ReservationService.java`
- Controller: map endpoint và trả response.
- Service: toàn bộ nghiệp vụ booking, validate date-range, check quyền, transition status.

---

## 7) Phụ lục: gợi ý tách tài liệu “từng dòng 100%”

Nếu cần đúng mức “từng dòng/từng biểu thức” cho toàn dự án, đề xuất tách thành 5 file:
1. `DOC_AUTH.md`
2. `DOC_USERS.md`
3. `DOC_HOTELS.md`
4. `DOC_ROOMS.md`
5. `DOC_RESERVATIONS.md`

Mỗi file sẽ:
- liệt kê class -> method -> giải thích từng đoạn code theo line range
- giải thích vì sao dùng annotation đó
- mapping request/response ví dụ

