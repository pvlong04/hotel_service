# Booking/Reservation Guide

Tai lieu nay mo ta chi tiet luong Booking hoan chinh duoc trien khai tren module `Reservation`.

## 1) API da hoan thien

### Booking/Reservation APIs (`/reservations`)

- `POST /reservations/availability`: Kiem tra phong trong theo ngay va hotel
- `POST /reservations`: Tao don dat phong
- `GET /reservations/{reservationId}`: Lay chi tiet mot don dat phong
- `GET /reservations/me`: Lay danh sach don dat cua user hien tai
- `GET /reservations?hotelId=&status=`: Lay danh sach don dat (STAFF/ADMIN)
- `PATCH /reservations/{reservationId}/status`: Chuyen trang thai don dat
- `POST /reservations/payments`: Ghi nhan thanh toan cho don dat
- `POST /reservations/charges`: Them phu phi cho don dat (STAFF/ADMIN)

### Trang thai nghiep vu

- Reservation: `PENDING -> CONFIRMED -> CHECKED_IN -> CHECKED_OUT`
- Co the `CANCELLED` tu `PENDING` hoac `CONFIRMED`
- Guest chi duoc phep huy don cua chinh minh

## 2) Luong hoat dong he thong

1. User dang nhap nhan JWT (`userId`, `roles`).
2. User kiem tra phong trong theo ngay den/di.
3. User tao reservation, he thong chot danh sach phong va gia snapshot.
4. STAFF/ADMIN xac nhan (`CONFIRMED`) don dat.
5. Den ngay nhan phong, STAFF check-in (`CHECKED_IN`) va doi trang thai room -> `OCCUPIED`.
6. Trong ky luu tru co the thu tien (`payments`) va them phu phi (`charges`).
7. Tra phong (`CHECKED_OUT`) thi room -> `AVAILABLE` va khoa don.

## 3) Giai thich chi tiet code vua bo sung

### `src/main/java/org/example/hotel_service/services/reservation/ReservationService.java`

- Khoi tao service voi cac repository can thiet: reservation, room, payment, charge, user, hotel, room type.
- `checkAvailability(...)`
  - Validate ngay (`checkIn < checkOut`).
  - Kiem tra hotel ton tai.
  - Goi `RoomRepository.findAvailableRooms(...)` de lay danh sach phong khong bi overlap.
  - Loc them theo `roomTypeId` va suc chua (`adults + children`).
  - Map ve `RoomResponse`.
- `createReservation(...)`
  - Lay `userId` tu JWT va tai user guest.
  - Validate hotel, date-range va so dem.
  - Lay danh sach phong trong.
  - Duyet tung item room request:
    - Kiem tra room type thuoc dung hotel.
    - Neu co `roomId` thi bat buoc phong do dang trong va dung room type.
    - Neu khong co `roomId` thi auto-pick phong trong dau tien dung room type.
  - Tao `ReservationItem` voi `ratePerNight` snapshot va `amount = rate * nights`.
  - Cong don `totalAmount`, luu reservation, tra `ReservationCreatedResponse`.
- `getReservationById(...)`
  - Load reservation bang entity graph (co items/payments/charges).
  - Kiem tra quyen doc: guest chi duoc xem don cua minh; staff/admin xem tat ca.
- `getMyReservations(...)`
  - Lay theo `guest_user_id` cua JWT.
- `getReservations(...)`
  - Bat buoc STAFF/ADMIN.
  - Ho tro loc theo `hotelId`, `status`.
- `updateReservationStatus(...)`
  - Validate request status.
  - Guest chi duoc huy don cua chinh minh va chi khi `PENDING/CONFIRMED`.
  - STAFF/ADMIN duoc chuyen theo matrix hop le.
  - Khi check-in/check-out/cancel, dong bo trang thai `ReservationItem` va `Room`.
- `createPayment(...)`
  - Xac thuc quyen truy cap reservation.
  - Khong cho payment neu reservation da `CANCELLED`.
  - Validate amount > 0 va khong vuot `remaining`.
  - Tao `Payment` status `COMPLETED`, cap nhat `paidAmount`.
- `createCharge(...)`
  - Chi STAFF/ADMIN.
  - Khong cho them charge neu reservation `CANCELLED`/`CHECKED_OUT`.
  - Tao `ReservationCharge`, tang `totalAmount`.
- Cac helper:
  - `applyStatusTransition(...)`: xu ly timestamp, actor, trang thai room/item.
  - `isValidTransition(...)`: matrix chuyen trang thai.
  - `extractUserId(...)`, `extractRoles(...)`: doc claims JWT.
  - `generateReservationCode(...)`: tao ma reservation co timestamp + suffix nano.

### `src/main/java/org/example/hotel_service/controllers/ReservationController.java`

- Expose day du endpoint REST cho booking flow.
- Moi endpoint deu wrap ket qua bang `ApiResponse`.
- Endpoint mutating du lieu deu nhan `@AuthenticationPrincipal Jwt`.

### `src/main/java/org/example/hotel_service/services/booking/BookingServiceImp.java`

- Da sua method signature ve `previewBooking(...)` de dong bo interface/implementation.

### `src/main/java/org/example/hotel_service/services/booking/BookingService.java`

- Da doi ten method override thanh `previewBooking(...)` de fix loi.

### `src/main/java/org/example/hotel_service/exception/ErrorCode.java`

- Bo sung ma loi cho reservation/payment:
  - `RESERVATION_NOT_FOUND`
  - `RESERVATION_ACCESS_DENIED`
  - `RESERVATION_STATUS_TRANSITION_INVALID`
  - `PAYMENT_AMOUNT_INVALID`

## 4) Kich ban test API de verify nhanh

1. Dang nhap guest lay token.
2. Goi `POST /reservations/availability` voi range ngay hop le.
3. Goi `POST /reservations` tao don dat.
4. Goi `GET /reservations/me` kiem tra don vua tao.
5. Dang nhap staff/admin, `PATCH /reservations/{id}/status` sang `CONFIRMED` -> `CHECKED_IN` -> `CHECKED_OUT`.
6. Trinh tu bo sung:
   - `POST /reservations/charges` khi dang `CHECKED_IN`.
   - `POST /reservations/payments` de cap nhat `paidAmount`.

## 5) Luu y van hanh

- Moi request vao endpoint private can JWT hop le.
- Guest khong duoc xem/sua don cua nguoi khac.
- He thong dang dung `reservation` lam nguon su that cho booking, module `Booking` cu giu vai tro legacy.

