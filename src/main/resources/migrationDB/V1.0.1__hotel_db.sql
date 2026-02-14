-- =========================================================
-- HOTEL SERVICE DATABASE - OPTIMIZED VERSION
-- =========================================================
-- Tối ưu bởi: AI Assistant
-- Ngày: 2026-02-04
--
-- CÁC CẢI TIẾN SO VỚI PHIÊN BẢN GỐC:
-- 1. Thêm bảng room_type_images thay vì room_images (hình ảnh theo loại phòng hợp lý hơn)
-- 2. Thêm bảng hotel_images cho hình ảnh khách sạn
-- 3. Thêm trường gender trong profiles
-- 4. Thêm cancel_reason trong reservations
-- 5. Thêm actual_check_in/actual_check_out để phân biệt ngày dự kiến và thực tế
-- 6. Thêm số người lớn/trẻ em trong reservation (adults, children)
-- 7. Thêm special_requests cho yêu cầu đặc biệt
-- 8. Thêm index cho tìm kiếm theo ngày check-in/check-out
-- 9. Thêm bảng reviews cho đánh giá khách sạn
-- 10. Chuẩn hóa updated_at cho tất cả các bảng cần thiết
-- =========================================================

DROP DATABASE IF EXISTS hotel_db;
CREATE DATABASE hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE hotel_db;

SET sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- =========================================================
-- 1) AUTH & RBAC - Xác thực và phân quyền
-- =========================================================

-- Bảng USERS: Lưu thông tin đăng nhập người dùng
-- Giải thích:
--   user_id: ID tự tăng, khóa chính
--   email: Email đăng nhập, duy nhất trong hệ thống
--   password_hash: Mật khẩu đã được hash (bcrypt/argon2)
--   status: Trạng thái tài khoản
--     - PENDING: Chờ xác thực email
--     - ACTIVE: Đã kích hoạt, có thể đăng nhập
--     - BANNED: Bị khóa, không thể đăng nhập
--   last_login_at: Thời điểm đăng nhập lần cuối (để tracking)
CREATE TABLE users (
                       user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(100) NOT NULL ,
                       email VARCHAR(150) ,
                       password_hash VARCHAR(255) NOT NULL,
                       status ENUM('PENDING','ACTIVE','BANNED') NOT NULL DEFAULT 'PENDING',
                       last_login_at DATETIME DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       INDEX idx_users_status (status),
                       INDEX idx_users_email_status (username, status)
) ENGINE=InnoDB COMMENT='Bảng người dùng - lưu thông tin đăng nhập';

-- Bảng PROFILES: Thông tin cá nhân của người dùng
-- Tách riêng khỏi users để:
--   1. Tối ưu query khi chỉ cần thông tin đăng nhập
--   2. Dễ mở rộng thông tin cá nhân mà không ảnh hưởng bảng users
-- Giải thích:
--   gender: Giới tính (MALE/FEMALE/OTHER) - thêm mới để phục vụ báo cáo
--   id_card_number: Số CMND/CCCD - cần thiết cho check-in khách sạn
--   nationality: Quốc tịch - quan trọng cho khách quốc tế
CREATE TABLE profiles (
                          profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT NOT NULL UNIQUE,
                          full_name VARCHAR(120) NOT NULL,
                          phone VARCHAR(30) UNIQUE DEFAULT NULL,
                          avatar_url VARCHAR(500) DEFAULT NULL,
                          address VARCHAR(255) DEFAULT NULL,
                          dob DATE DEFAULT NULL,
                          gender ENUM('MALE','FEMALE','OTHER') DEFAULT NULL,
                          id_card_number VARCHAR(50) DEFAULT NULL,
                          nationality VARCHAR(100) DEFAULT 'Vietnam',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          INDEX idx_profiles_phone (phone),
                          INDEX idx_profiles_fullname (full_name),
                          CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
                              REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Thông tin cá nhân người dùng';

-- Bảng ROLES: Các vai trò trong hệ thống
-- Sử dụng ENUM để giới hạn vai trò cố định:
--   ADMIN: Quản trị viên toàn quyền
--   STAFF: Nhân viên khách sạn (lễ tân, quản lý)
--   GUEST: Khách hàng đặt phòng
CREATE TABLE roles (
                       role_id INT AUTO_INCREMENT PRIMARY KEY,
                       name ENUM('ADMIN','STAFF','GUEST') NOT NULL UNIQUE,
                       description VARCHAR(255) DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Bảng vai trò người dùng';

-- Bảng USER_ROLES: Liên kết nhiều-nhiều giữa users và roles
-- Một user có thể có nhiều role (VD: vừa là STAFF vừa là GUEST)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, role_id),

                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
                                REFERENCES users(user_id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id)
                                REFERENCES roles(role_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Bảng phân quyền người dùng';

-- Bảng AUTH_TOKENS: Token xác thực (email, reset password)
-- Giải thích:
--   purpose: Mục đích của token
--     - VERIFY_EMAIL: Xác thực email qua link
--     - VERIFY_EMAIL_OTP: Xác thực email qua mã OTP
--     - RESET_PASSWORD: Đặt lại mật khẩu
--   token_hash: Hash của token (không lưu plaintext)
--   otp_fail_count: Số lần nhập OTP sai (để chống brute force)
--   used_at: Thời điểm sử dụng (NULL = chưa dùng)
CREATE TABLE auth_tokens (
                             token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             purpose ENUM('VERIFY_EMAIL','VERIFY_EMAIL_OTP','RESET_PASSWORD') NOT NULL DEFAULT 'RESET_PASSWORD',
                             token_hash VARCHAR(255) NOT NULL,
                             expires_at DATETIME NOT NULL,
                             used_at DATETIME DEFAULT NULL,
                             otp_fail_count INT NOT NULL DEFAULT 0,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                             UNIQUE KEY uq_auth_token_hash (token_hash),
                             INDEX idx_auth_user_purpose (user_id, purpose),
                             INDEX idx_auth_expires (expires_at),
                             CONSTRAINT fk_auth_user FOREIGN KEY (user_id)
                                 REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Token xác thực email và reset password';

-- Bảng REFRESH_TOKENS: Refresh token cho JWT authentication
-- Giải thích:
--   token_hash: Hash của refresh token
--   revoked_at: Thời điểm thu hồi (logout hoặc bị force logout)
--   user_agent: Thông tin trình duyệt/thiết bị
--   ip_address: Địa chỉ IP đăng nhập (để phát hiện bất thường)
CREATE TABLE refresh_tokens (
                                rt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(255) NOT NULL,
                                expires_at DATETIME NOT NULL,
                                revoked_at DATETIME DEFAULT NULL,
                                user_agent VARCHAR(500) DEFAULT NULL,
                                ip_address VARCHAR(64) DEFAULT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                INDEX idx_rt_user (user_id),
                                INDEX idx_rt_expires (expires_at),
                                INDEX idx_rt_token_hash (token_hash),
                                CONSTRAINT fk_rt_user FOREIGN KEY (user_id)
                                    REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Refresh token cho JWT';


-- =========================================================
-- 2) HOTEL / FLOOR / ROOM TYPE / ROOM / AMENITIES
-- =========================================================

-- Bảng HOTELS: Thông tin khách sạn
-- Hỗ trợ multi-hotel (chuỗi khách sạn)
-- Giải thích:
--   star_rating: Số sao khách sạn (1-5)
--   description: Mô tả chi tiết về khách sạn
--   check_in_time/check_out_time: Giờ check-in/out tiêu chuẩn
--   latitude/longitude: Tọa độ GPS cho bản đồ
CREATE TABLE hotels (
                        hotel_id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(500) DEFAULT NULL,
                        phone VARCHAR(64) DEFAULT NULL,
                        email VARCHAR(150) DEFAULT NULL,
                        description TEXT DEFAULT NULL,
                        star_rating TINYINT DEFAULT 3 CHECK (star_rating BETWEEN 1 AND 5),
                        check_in_time TIME DEFAULT '14:00:00',
                        check_out_time TIME DEFAULT '12:00:00',
                        latitude DECIMAL(10, 8) DEFAULT NULL,
                        longitude DECIMAL(11, 8) DEFAULT NULL,
                        timezone VARCHAR(64) DEFAULT 'Asia/Ho_Chi_Minh',
                        status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        INDEX idx_hotels_status (status),
                        INDEX idx_hotels_location (latitude, longitude)
) ENGINE=InnoDB COMMENT='Bảng thông tin khách sạn';

-- Bảng HOTEL_IMAGES: Hình ảnh khách sạn (thêm mới)
-- Lưu nhiều hình ảnh cho mỗi khách sạn
-- Giải thích:
--   image_type: Loại hình ảnh (EXTERIOR/LOBBY/RESTAURANT/POOL/OTHER)
--   display_order: Thứ tự hiển thị
--   is_primary: Có phải ảnh đại diện không
CREATE TABLE hotel_images (
                              image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              hotel_id INT NOT NULL,
                              url VARCHAR(500) NOT NULL,
                              image_type ENUM('EXTERIOR','LOBBY','RESTAURANT','POOL','FACILITY','OTHER') DEFAULT 'OTHER',
                              display_order INT DEFAULT 0,
                              is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                              alt_text VARCHAR(255) DEFAULT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                              INDEX idx_hotel_images_hotel (hotel_id),
                              CONSTRAINT fk_hi_hotel FOREIGN KEY (hotel_id)
                                  REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Hình ảnh khách sạn';

-- Bảng FLOORS: Các tầng trong khách sạn
-- Giải thích:
--   code: Mã tầng (F1, F2, B1 cho tầng hầm...)
--   name: Tên hiển thị (Tầng 1, Tầng Trệt...)
--   floor_order: Thứ tự tầng (để sắp xếp hiển thị)
CREATE TABLE floors (
                        floor_id INT AUTO_INCREMENT PRIMARY KEY,
                        hotel_id INT NOT NULL,
                        code VARCHAR(40) NOT NULL,
                        name VARCHAR(120) DEFAULT NULL,
                        floor_order INT DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                        UNIQUE KEY uq_floor_code_hotel (hotel_id, code),
                        CONSTRAINT fk_floor_hotel FOREIGN KEY (hotel_id)
                            REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Các tầng trong khách sạn';

-- Bảng ROOM_TYPES: Các loại phòng
-- Giải thích:
--   code: Mã loại phòng (STD, DLX, STE...)
--   capacity: Số người tối đa
--   base_price: Giá gốc (đơn vị nhỏ nhất VD: VND)
--   weekend_price: Giá cuối tuần (thường cao hơn)
--   extra_bed_price: Phí giường phụ
--   total_rooms/available_rooms: Được tự động cập nhật qua trigger
--   bed_type: Loại giường (SINGLE/DOUBLE/TWIN/KING/QUEEN)
--   room_size: Diện tích phòng (m²)
CREATE TABLE room_types (
                            room_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            hotel_id INT NOT NULL,
                            code VARCHAR(50) NOT NULL,
                            name VARCHAR(150) NOT NULL,
                            description TEXT DEFAULT NULL,
                            capacity INT NOT NULL DEFAULT 2,
                            max_adults INT NOT NULL DEFAULT 2,
                            max_children INT NOT NULL DEFAULT 1,
                            base_price INT NOT NULL DEFAULT 0 COMMENT 'Giá cơ bản theo đơn vị tiền tệ nhỏ nhất (VND)',
                            weekend_price INT DEFAULT NULL COMMENT 'Giá cuối tuần, NULL = dùng base_price',
                            extra_bed_price INT DEFAULT 0 COMMENT 'Phí giường phụ',
                            bed_type ENUM('SINGLE','DOUBLE','TWIN','KING','QUEEN') DEFAULT 'DOUBLE',
                            room_size DECIMAL(5,2) DEFAULT NULL COMMENT 'Diện tích phòng (m²)',
                            status ENUM('ACTIVE','INACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
                            total_rooms INT NOT NULL DEFAULT 0 COMMENT 'Tổng số phòng (auto-updated by trigger)',
                            available_rooms INT NOT NULL DEFAULT 0 COMMENT 'Số phòng trống (auto-updated by trigger)',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            UNIQUE KEY uq_room_type_code_hotel (hotel_id, code),
                            INDEX idx_rt_hotel (hotel_id),
                            INDEX idx_rt_status (status),
                            INDEX idx_rt_price (base_price),
                            CONSTRAINT fk_rt_hotel FOREIGN KEY (hotel_id)
                                REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Các loại phòng trong khách sạn';

-- Bảng ROOM_TYPE_IMAGES: Hình ảnh theo loại phòng (thay cho room_images)
-- Lý do: Các phòng cùng loại thường có hình ảnh giống nhau
-- Tiết kiệm storage và dễ quản lý hơn room_images
CREATE TABLE room_type_images (
                                  image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  room_type_id BIGINT NOT NULL,
                                  url VARCHAR(500) NOT NULL,
                                  display_order INT DEFAULT 0,
                                  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                                  alt_text VARCHAR(255) DEFAULT NULL,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                  INDEX idx_rti_room_type (room_type_id),
                                  CONSTRAINT fk_rti_room_type FOREIGN KEY (room_type_id)
                                      REFERENCES room_types(room_type_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Hình ảnh theo loại phòng';

-- Bảng AMENITIES: Các tiện nghi
-- Amenities có thể áp dụng cho khách sạn hoặc loại phòng
-- Giải thích:
--   icon: Tên icon (font-awesome hoặc custom)
--   category: Phân loại tiện nghi
CREATE TABLE amenities (
                           amenity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(120) NOT NULL,
                           description VARCHAR(255) DEFAULT NULL,
                           icon VARCHAR(100) DEFAULT NULL,
                           category ENUM('ROOM','BATHROOM','ENTERTAINMENT','FOOD','FACILITY','SERVICE') DEFAULT 'ROOM',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                           UNIQUE KEY uq_amenity_name (name),
                           INDEX idx_amenity_category (category)
) ENGINE=InnoDB COMMENT='Danh sách tiện nghi';

-- Bảng ROOM_TYPE_AMENITIES: Liên kết loại phòng và tiện nghi
CREATE TABLE room_type_amenities (
                                     room_type_id BIGINT NOT NULL,
                                     amenity_id BIGINT NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (room_type_id, amenity_id),

                                     CONSTRAINT fk_rta_rt FOREIGN KEY (room_type_id)
                                         REFERENCES room_types(room_type_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_rta_amenity FOREIGN KEY (amenity_id)
                                         REFERENCES amenities(amenity_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Tiện nghi của từng loại phòng';

-- Bảng ROOMS: Các phòng cụ thể
-- Giải thích:
--   room_number: Số phòng (101, 102, 201...)
--   status: Trạng thái phòng
--     - AVAILABLE: Trống, có thể đặt
--     - HELD: Đang giữ chỗ (chờ thanh toán)
--     - OCCUPIED: Đang có khách
--     - MAINTENANCE: Đang bảo trì
--     - REMOVED: Đã ngừng sử dụng
--   note: Ghi chú nội bộ (VD: "Điều hòa hỏng")
CREATE TABLE rooms (
                       room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       hotel_id INT NOT NULL,
                       room_number VARCHAR(50) NOT NULL,
                       room_type_id BIGINT NOT NULL,
                       floor_id INT DEFAULT NULL,
                       status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL DEFAULT 'AVAILABLE',
                       note VARCHAR(500) DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       INDEX idx_rooms_hotel (hotel_id),
                       INDEX idx_rooms_type (room_type_id),
                       INDEX idx_rooms_status (status),
                       INDEX idx_rooms_type_status (room_type_id, status),
                       UNIQUE KEY uq_room_number_hotel (hotel_id, room_number),
                       CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id)
                           REFERENCES hotels(hotel_id) ON DELETE CASCADE,
                       CONSTRAINT fk_room_type FOREIGN KEY (room_type_id)
                           REFERENCES room_types(room_type_id) ON DELETE RESTRICT,
                       CONSTRAINT fk_room_floor FOREIGN KEY (floor_id)
                           REFERENCES floors(floor_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Các phòng cụ thể trong khách sạn';


-- =========================================================
-- 3) RESERVATIONS / BOOKING / ITEMS / PAYMENTS / NOTIFICATIONS
-- =========================================================

-- Bảng RESERVATIONS: Đơn đặt phòng
-- Giải thích:
--   reservation_code: Mã đặt phòng duy nhất (hiển thị cho khách)
--   status: Trạng thái đặt phòng
--     - PENDING: Chờ xác nhận
--     - CONFIRMED: Đã xác nhận
--     - CHECKED_IN: Đã nhận phòng
--     - CHECKED_OUT: Đã trả phòng
--     - CANCELLED: Đã hủy
--     - NO_SHOW: Khách không đến
--   check_in_date/check_out_date: Ngày dự kiến
--   actual_check_in/actual_check_out: Thời điểm thực tế (có giờ)
--   adults/children: Số người lớn/trẻ em
--   special_requests: Yêu cầu đặc biệt (VD: phòng tầng cao, không hút thuốc)
--   cancel_reason: Lý do hủy (nếu có)
CREATE TABLE reservations (
                              reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              reservation_code VARCHAR(40) NOT NULL UNIQUE,
                              guest_id BIGINT NOT NULL,
                              hotel_id INT NOT NULL,
                              status ENUM('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'PENDING',

    -- Thời gian yêu cầu và xử lý
                              requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              approved_at DATETIME DEFAULT NULL,
                              approved_by BIGINT DEFAULT NULL,

    -- Ngày check-in/out dự kiến
                              check_in_date DATE NOT NULL,
                              check_out_date DATE NOT NULL,
                              nights_count INT NOT NULL DEFAULT 1,

    -- Thời điểm check-in/out thực tế
                              actual_check_in DATETIME DEFAULT NULL,
                              actual_check_out DATETIME DEFAULT NULL,

    -- Số người
                              adults INT NOT NULL DEFAULT 1,
                              children INT NOT NULL DEFAULT 0,

    -- Tài chính
                              total_amount INT NOT NULL DEFAULT 0,
                              paid_amount INT NOT NULL DEFAULT 0,
                              discount_amount INT NOT NULL DEFAULT 0,

    -- Yêu cầu và ghi chú
                              special_requests TEXT DEFAULT NULL,
                              internal_notes TEXT DEFAULT NULL,

    -- Hủy
                              cancelled_at DATETIME DEFAULT NULL,
                              cancel_reason VARCHAR(500) DEFAULT NULL,

                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                              INDEX idx_res_guest (guest_id),
                              INDEX idx_res_hotel (hotel_id),
                              INDEX idx_res_status (status),
                              INDEX idx_res_code (reservation_code),
                              INDEX idx_res_checkin (check_in_date),
                              INDEX idx_res_checkout (check_out_date),
                              INDEX idx_res_date_range (hotel_id, check_in_date, check_out_date),

                              CONSTRAINT fk_res_guest FOREIGN KEY (guest_id)
                                  REFERENCES users(user_id) ON DELETE RESTRICT,
                              CONSTRAINT fk_res_hotel FOREIGN KEY (hotel_id)
                                  REFERENCES hotels(hotel_id) ON DELETE CASCADE,
                              CONSTRAINT fk_res_approved_by FOREIGN KEY (approved_by)
                                  REFERENCES users(user_id) ON DELETE SET NULL,
                              CONSTRAINT chk_dates CHECK (check_out_date > check_in_date),
                              CONSTRAINT chk_nights CHECK (nights_count > 0)
) ENGINE=InnoDB COMMENT='Đơn đặt phòng';

-- Bảng RESERVATION_ITEMS: Chi tiết từng phòng trong đơn đặt
-- Một đơn đặt có thể có nhiều phòng
-- Giải thích:
--   rate_per_night: Giá phòng tại thời điểm đặt (có thể khác giá hiện tại)
--   amount: Tổng tiền = rate_per_night * nights
CREATE TABLE reservation_items (
                                   reservation_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   reservation_id BIGINT NOT NULL,
                                   room_id BIGINT NOT NULL,
                                   room_type_id BIGINT NOT NULL,
                                   rate_per_night INT NOT NULL DEFAULT 0,
                                   nights INT NOT NULL DEFAULT 1,
                                   amount INT NOT NULL DEFAULT 0,
                                   status ENUM('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'BOOKED',
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                   UNIQUE KEY uq_res_room (reservation_id, room_id),
                                   INDEX idx_ri_res (reservation_id),
                                   INDEX idx_ri_room (room_id),

                                   CONSTRAINT fk_ri_res FOREIGN KEY (reservation_id)
                                       REFERENCES reservations(reservation_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_ri_room FOREIGN KEY (room_id)
                                       REFERENCES rooms(room_id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_ri_rt FOREIGN KEY (room_type_id)
                                       REFERENCES room_types(room_type_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Chi tiết phòng trong đơn đặt';

-- Bảng RESERVATION_CHARGES: Các chi phí phát sinh
-- VD: minibar, room service, giặt ủi, phí hủy...
-- Giải thích:
--   charge_type: Loại chi phí để phân loại
--   quantity: Số lượng
--   unit_price: Đơn giá
CREATE TABLE reservation_charges (
                                     charge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     reservation_id BIGINT NOT NULL,
                                     charge_type ENUM('EXTRA_BED','MINIBAR','ROOM_SERVICE','LAUNDRY','DAMAGE','LATE_CHECKOUT','CANCEL_FEE','OTHER') DEFAULT 'OTHER',
                                     description VARCHAR(255) DEFAULT NULL,
                                     quantity INT NOT NULL DEFAULT 1,
                                     unit_price INT NOT NULL DEFAULT 0,
                                     amount INT NOT NULL DEFAULT 0,
                                     created_by BIGINT DEFAULT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                     INDEX idx_charge_res (reservation_id),
                                     CONSTRAINT fk_charge_res FOREIGN KEY (reservation_id)
                                         REFERENCES reservations(reservation_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_charge_user FOREIGN KEY (created_by)
                                         REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Chi phí phát sinh trong đặt phòng';

-- Bảng PAYMENTS: Thanh toán
-- Giải thích:
--   method: Phương thức thanh toán
--   provider: Nhà cung cấp (VNPay, Momo, Stripe...)
--   provider_trans_id: Mã giao dịch từ provider
--   refund_amount: Số tiền hoàn (nếu có)
CREATE TABLE payments (
                          payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          reservation_id BIGINT DEFAULT NULL,
                          guest_id BIGINT NOT NULL,
                          amount INT NOT NULL,
                          method ENUM('CARD','CASH','BANK_TRANSFER','E_WALLET','ONLINE') NOT NULL DEFAULT 'ONLINE',
                          provider VARCHAR(100) DEFAULT NULL,
                          provider_trans_id VARCHAR(255) DEFAULT NULL,
                          status ENUM('PENDING','COMPLETED','FAILED','REFUNDED','PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
                          refund_amount INT DEFAULT 0,
                          paid_at DATETIME DEFAULT NULL,
                          notes VARCHAR(500) DEFAULT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          INDEX idx_pay_res (reservation_id),
                          INDEX idx_pay_guest (guest_id),
                          INDEX idx_pay_status (status),
                          INDEX idx_pay_provider_trans (provider_trans_id),

                          CONSTRAINT fk_pay_res FOREIGN KEY (reservation_id)
                              REFERENCES reservations(reservation_id) ON DELETE SET NULL,
                          CONSTRAINT fk_pay_guest FOREIGN KEY (guest_id)
                              REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Thanh toán';

-- Bảng NOTIFICATIONS: Thông báo cho người dùng
-- Giải thích:
--   type: Loại thông báo
--   reference_id: ID liên quan (VD: reservation_id)
--   is_read: Đã đọc chưa
--   read_at: Thời điểm đọc
CREATE TABLE notifications (
                               notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               type ENUM(
                                   'RESERVATION_CREATED',
                                   'RESERVATION_CONFIRMED',
                                   'RESERVATION_CHECKIN',
                                   'RESERVATION_CHECKOUT',
                                   'RESERVATION_CANCELLED',
                                   'RESERVATION_REMINDER',
                                   'PAYMENT_SUCCESS',
                                   'PAYMENT_FAILED',
                                   'REVIEW_REQUEST',
                                   'PROMOTION',
                                   'SYSTEM'
                                   ) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               content TEXT,
                               reference_id BIGINT DEFAULT NULL,
                               reference_type VARCHAR(50) DEFAULT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at DATETIME DEFAULT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               INDEX idx_notification_user_read (user_id, is_read),
                               INDEX idx_notification_created (created_at),
                               INDEX idx_notification_type (type),

                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id)
                                   REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Thông báo người dùng';


-- =========================================================
-- 4) REVIEWS - Đánh giá khách sạn (THÊM MỚI)
-- =========================================================

-- Bảng REVIEWS: Đánh giá từ khách
-- Chỉ cho phép đánh giá sau khi đã checkout
-- Giải thích:
--   rating: Điểm đánh giá tổng thể (1-5)
--   cleanliness_rating/service_rating/...: Điểm chi tiết từng mục
--   is_verified: Đã xác minh (thực sự ở khách sạn)
--   is_visible: Có hiển thị công khai không (admin có thể ẩn review vi phạm)
CREATE TABLE reviews (
                         review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         reservation_id BIGINT NOT NULL UNIQUE,
                         guest_id BIGINT NOT NULL,
                         hotel_id INT NOT NULL,

    -- Đánh giá tổng thể
                         rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         title VARCHAR(255) DEFAULT NULL,
                         content TEXT DEFAULT NULL,

    -- Đánh giá chi tiết (optional)
                         cleanliness_rating TINYINT DEFAULT NULL CHECK (cleanliness_rating BETWEEN 1 AND 5),
                         service_rating TINYINT DEFAULT NULL CHECK (service_rating BETWEEN 1 AND 5),
                         location_rating TINYINT DEFAULT NULL CHECK (location_rating BETWEEN 1 AND 5),
                         facilities_rating TINYINT DEFAULT NULL CHECK (facilities_rating BETWEEN 1 AND 5),
                         value_rating TINYINT DEFAULT NULL CHECK (value_rating BETWEEN 1 AND 5),

    -- Trạng thái
                         is_verified BOOLEAN NOT NULL DEFAULT TRUE,
                         is_visible BOOLEAN NOT NULL DEFAULT TRUE,

    -- Phản hồi từ khách sạn
                         reply TEXT DEFAULT NULL,
                         replied_at DATETIME DEFAULT NULL,
                         replied_by BIGINT DEFAULT NULL,

                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         INDEX idx_review_hotel (hotel_id),
                         INDEX idx_review_guest (guest_id),
                         INDEX idx_review_rating (rating),
                         INDEX idx_review_visible (is_visible),

                         CONSTRAINT fk_review_res FOREIGN KEY (reservation_id)
                             REFERENCES reservations(reservation_id) ON DELETE CASCADE,
                         CONSTRAINT fk_review_guest FOREIGN KEY (guest_id)
                             REFERENCES users(user_id) ON DELETE CASCADE,
                         CONSTRAINT fk_review_hotel FOREIGN KEY (hotel_id)
                             REFERENCES hotels(hotel_id) ON DELETE CASCADE,
                         CONSTRAINT fk_review_replied_by FOREIGN KEY (replied_by)
                             REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Đánh giá từ khách hàng';


-- =========================================================
-- 5) ROOM AVAILABILITY LOG - Lịch sử thay đổi trạng thái phòng
-- =========================================================

-- Bảng ROOM_AVAILABILITY_LOGS: Ghi log mỗi khi trạng thái phòng thay đổi
-- Dùng để: audit, debug, báo cáo thống kê
CREATE TABLE room_availability_logs (
                                        log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        room_id BIGINT NOT NULL,
                                        old_status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL,
                                        new_status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL,
                                        reason VARCHAR(255) DEFAULT NULL,
                                        reservation_id BIGINT DEFAULT NULL,
                                        changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        changed_by BIGINT DEFAULT NULL,

                                        INDEX idx_ral_room (room_id),
                                        INDEX idx_ral_changed_at (changed_at),

                                        CONSTRAINT fk_ral_room FOREIGN KEY (room_id)
                                            REFERENCES rooms(room_id) ON DELETE CASCADE,
                                        CONSTRAINT fk_ral_user FOREIGN KEY (changed_by)
                                            REFERENCES users(user_id) ON DELETE SET NULL,
                                        CONSTRAINT fk_ral_reservation FOREIGN KEY (reservation_id)
                                            REFERENCES reservations(reservation_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Lịch sử thay đổi trạng thái phòng';


-- =========================================================
-- 6) TRIGGERS - Tự động cập nhật số phòng
-- =========================================================

-- Trigger: Khi thêm phòng mới
DELIMITER $$

CREATE TRIGGER trg_rooms_after_insert
    AFTER INSERT ON rooms
    FOR EACH ROW
BEGIN
    UPDATE room_types
    SET total_rooms = total_rooms + CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END,
        available_rooms = available_rooms + CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
    WHERE room_type_id = NEW.room_type_id;
END$$

-- Trigger: Khi xóa phòng
CREATE TRIGGER trg_rooms_after_delete
    AFTER DELETE ON rooms
    FOR EACH ROW
BEGIN
    UPDATE room_types
    SET total_rooms = total_rooms - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END,
        available_rooms = available_rooms - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END
    WHERE room_type_id = OLD.room_type_id;
END$$

-- Trigger: Khi cập nhật phòng
CREATE TRIGGER trg_rooms_after_update
    AFTER UPDATE ON rooms
    FOR EACH ROW
BEGIN
    IF OLD.room_type_id <> NEW.room_type_id THEN
        -- Giảm số lượng ở room_type cũ
        UPDATE room_types
        SET total_rooms = total_rooms - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END,
            available_rooms = available_rooms - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END
        WHERE room_type_id = OLD.room_type_id;

        -- Tăng số lượng ở room_type mới
        UPDATE room_types
        SET total_rooms = total_rooms + CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END,
            available_rooms = available_rooms + CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
        WHERE room_type_id = NEW.room_type_id;
    ELSE
        -- Cùng room_type: chỉ điều chỉnh delta
        UPDATE room_types
        SET total_rooms = total_rooms
            + (CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END
                - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END),
            available_rooms = available_rooms
                + (CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
                    - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END)
        WHERE room_type_id = NEW.room_type_id;
    END IF;
END$$

DELIMITER ;


-- =========================================================
-- 7) SEED DATA - Dữ liệu mẫu
-- =========================================================

-- Roles
INSERT INTO roles(name, description) VALUES
                                         ('ADMIN', 'Quản trị hệ thống - toàn quyền'),
                                         ('STAFF', 'Nhân viên khách sạn - quản lý đặt phòng, check-in/out'),
                                         ('GUEST', 'Khách hàng - đặt phòng, đánh giá');

-- Sample hotel
INSERT INTO hotels (name, address, phone, email, description, star_rating) VALUES
    ('Demo Hotel Central', '123 Example St, District 1, Ho Chi Minh City', '+84-123-456-789',
     'info@demo-hotel.example', 'Khách sạn hiện đại tại trung tâm thành phố', 4);

-- Floors
INSERT INTO floors (hotel_id, code, name, floor_order) VALUES
                                                           (1, 'G', 'Tầng Trệt', 0),
                                                           (1, 'F1', 'Tầng 1', 1),
                                                           (1, 'F2', 'Tầng 2', 2),
                                                           (1, 'F3', 'Tầng 3', 3);

-- Amenities với phân loại
INSERT INTO amenities (name, description, icon, category) VALUES
                                                              ('WiFi miễn phí', 'Internet không dây tốc độ cao', 'wifi', 'ROOM'),
                                                              ('Điều hòa', 'Máy lạnh 2 chiều', 'snowflake', 'ROOM'),
                                                              ('TV màn hình phẳng', 'Smart TV 43 inch', 'tv', 'ENTERTAINMENT'),
                                                              ('Minibar', 'Tủ lạnh mini với đồ uống', 'glass-martini', 'ROOM'),
                                                              ('Két sắt', 'Két an toàn trong phòng', 'lock', 'ROOM'),
                                                              ('Bồn tắm', 'Bồn tắm nằm', 'bath', 'BATHROOM'),
                                                              ('Vòi sen', 'Vòi sen riêng', 'shower', 'BATHROOM'),
                                                              ('Bữa sáng', 'Bữa sáng buffet miễn phí', 'utensils', 'FOOD'),
                                                              ('Hồ bơi', 'Hồ bơi ngoài trời', 'swimming-pool', 'FACILITY'),
                                                              ('Gym', 'Phòng tập thể dục', 'dumbbell', 'FACILITY'),
                                                              ('Spa', 'Dịch vụ spa và massage', 'spa', 'SERVICE'),
                                                              ('Đỗ xe', 'Bãi đỗ xe miễn phí', 'parking', 'SERVICE'),
                                                              ('Dịch vụ phòng 24/7', 'Room service 24 giờ', 'concierge-bell', 'SERVICE'),
                                                              ('Máy sấy tóc', 'Máy sấy tóc trong phòng tắm', 'wind', 'BATHROOM');

-- Room types
INSERT INTO room_types (hotel_id, code, name, description, capacity, max_adults, max_children, base_price, weekend_price, bed_type, room_size) VALUES
                                                                                                                                                   (1, 'STD', 'Standard', 'Phòng tiêu chuẩn, view thành phố', 2, 2, 1, 800000, 900000, 'DOUBLE', 25.00),
                                                                                                                                                   (1, 'SUP', 'Superior', 'Phòng Superior, rộng rãi hơn', 2, 2, 1, 1000000, 1150000, 'DOUBLE', 30.00),
                                                                                                                                                   (1, 'DLX', 'Deluxe', 'Phòng Deluxe với view đẹp', 3, 2, 2, 1500000, 1700000, 'KING', 35.00),
                                                                                                                                                   (1, 'STE', 'Suite', 'Phòng Suite cao cấp', 4, 3, 2, 2500000, 2800000, 'KING', 50.00);

-- Room type amenities
INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES
                                                               -- Standard: WiFi, Điều hòa, TV, Vòi sen
                                                               (1, 1), (1, 2), (1, 3), (1, 7),
                                                               -- Superior: + Minibar, Két sắt, Máy sấy
                                                               (2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 7), (2, 14),
                                                               -- Deluxe: + Bồn tắm, Bữa sáng
                                                               (3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6), (3, 7), (3, 8), (3, 14),
                                                               -- Suite: tất cả tiện nghi phòng
                                                               (4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 14);


-- =========================================================
-- NOTES: Hướng dẫn phát triển
-- =========================================================
--
-- 1. BUSINESS LOGIC trong Backend (không dùng trigger):
--    - Kiểm tra phòng available trước khi đặt
--    - Cập nhật room.status khi check-in/check-out
--    - Tính toán total_amount dựa trên nights và rate
--    - Gửi notification khi có sự kiện
--
-- 2. CÁC API CẦN THIẾT:
--    - Auth: register, login, refresh-token, forgot-password
--    - Hotels: CRUD, search với filter
--    - Rooms: CRUD, check availability
--    - Reservations: create, confirm, check-in, check-out, cancel
--    - Payments: create, callback từ payment gateway
--    - Reviews: create, reply
--
-- 3. BẢO MẬT:
--    - Hash password với bcrypt/argon2
--    - JWT với access token ngắn hạn + refresh token
--    - Rate limiting cho API
--    - Validate input và sanitize output
--
-- 4. HIỆU NĂNG:
--    - Pagination cho list API
--    - Cache với Redis cho data ít thay đổi
--    - Index đã được tạo cho các query phổ biến
-- =========================================================
