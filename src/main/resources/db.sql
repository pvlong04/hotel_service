-- DROP/CREATE database
DROP DATABASE IF EXISTS hotel_db;
CREATE DATABASE hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE hotel_db;

-- (Optional) strict mode
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- =========================================================
-- 1) AUTH & RBAC (giữ nguyên/điều chỉnh từ library)
-- =========================================================
CREATE TABLE users (
                       user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       status ENUM('PENDING','ACTIVE','BANNED') NOT NULL DEFAULT 'PENDING',
                       last_login_at DATETIME DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE profiles (
                          profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id BIGINT NOT NULL UNIQUE,
                          full_name VARCHAR(120) NOT NULL,
                          phone VARCHAR(30) UNIQUE DEFAULT NULL,
                          avatar_url VARCHAR(255) DEFAULT NULL,
                          address VARCHAR(255) DEFAULT NULL,
                          dob DATE DEFAULT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT fk_profiles_user
                              FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE roles (
                       role_id INT AUTO_INCREMENT PRIMARY KEY,
                       name ENUM('ADMIN','STAFF','GUEST') NOT NULL UNIQUE,
                       description VARCHAR(255) DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

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
                             CONSTRAINT fk_auth_user
                                 FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
                                rt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(255) NOT NULL,
                                expires_at DATETIME NOT NULL,
                                revoked_at DATETIME DEFAULT NULL,
                                user_agent VARCHAR(255) DEFAULT NULL,
                                ip_address VARCHAR(64) DEFAULT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                INDEX idx_rt_user (user_id),
                                INDEX idx_rt_expires (expires_at),
                                CONSTRAINT fk_rt_user
                                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =========================================================
-- 2) HOTEL / FLOOR / ROOM TYPE / ROOM / AMENITIES
-- =========================================================
CREATE TABLE hotels (
                        hotel_id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        address VARCHAR(255) DEFAULT NULL,
                        phone VARCHAR(64) DEFAULT NULL,
                        email VARCHAR(150) DEFAULT NULL,
                        timezone VARCHAR(64) DEFAULT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE floors (
                        floor_id INT AUTO_INCREMENT PRIMARY KEY,
                        hotel_id INT NOT NULL,
                        code VARCHAR(40) NOT NULL,
                        name VARCHAR(120) DEFAULT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uq_floor_code_hotel (hotel_id, code),
                        CONSTRAINT fk_floor_hotel
                            FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE room_types (
                            room_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            hotel_id INT NOT NULL,
                            code VARCHAR(50) NOT NULL,
                            name VARCHAR(150) NOT NULL,
                            description TEXT DEFAULT NULL,
                            capacity INT NOT NULL DEFAULT 1,
                            price_per_night INT NOT NULL DEFAULT 0, -- smallest currency unit (e.g. cents)
                            status ENUM('ACTIVE','INACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
                            total_rooms INT NOT NULL DEFAULT 0,
                            available_rooms INT NOT NULL DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uq_room_type_code_hotel (hotel_id, code),
                            INDEX idx_rt_hotel (hotel_id),
                            CONSTRAINT fk_rt_hotel
                                FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE amenities (
                           amenity_id INT AUTO_INCREMENT PRIMARY KEY,
                           name VARCHAR(120) NOT NULL,
                           description VARCHAR(255) DEFAULT NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           UNIQUE KEY uq_amenity_name (name)
) ENGINE=InnoDB;

CREATE TABLE room_type_amenities (
                                     room_type_id BIGINT NOT NULL,
                                     amenity_id INT NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (room_type_id, amenity_id),
                                     CONSTRAINT fk_rta_rt
                                         FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE CASCADE,
                                     CONSTRAINT fk_rta_amenity
                                         FOREIGN KEY (amenity_id) REFERENCES amenities(amenity_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE rooms (
                       room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       hotel_id INT NOT NULL,
                       room_number VARCHAR(50) NOT NULL,
                       room_type_id BIGINT NOT NULL,
                       floor_id INT DEFAULT NULL,
                       status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL DEFAULT 'AVAILABLE',
                       note VARCHAR(255) DEFAULT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       INDEX idx_rooms_hotel (hotel_id),
                       INDEX idx_rooms_type (room_type_id),
                       INDEX idx_rooms_status (status),
                       UNIQUE KEY uq_room_number_hotel (hotel_id, room_number),
                       CONSTRAINT fk_room_hotel
                           FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
                       CONSTRAINT fk_room_type
                           FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT,
                       CONSTRAINT fk_room_floor
                           FOREIGN KEY (floor_id) REFERENCES floors(floor_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE room_images (
                             image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             room_id BIGINT NOT NULL,
                             url VARCHAR(255) NOT NULL,
                             is_primary BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_ri_room
                                 FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =========================================================
-- 3) RESERVATIONS / BOOKING / ITEMS / PAYMENTS / NOTIFICATIONS
-- =========================================================
CREATE TABLE reservations (
                              reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              reservation_code VARCHAR(40) NOT NULL UNIQUE,
                              guest_id BIGINT NOT NULL, -- references users.user_id
                              hotel_id INT NOT NULL,
                              status ENUM('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'PENDING',
                              requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              approved_at DATETIME DEFAULT NULL,
                              approved_by BIGINT DEFAULT NULL,
                              check_in_date DATETIME DEFAULT NULL,
                              check_out_date DATETIME DEFAULT NULL,
                              nights_count INT DEFAULT NULL,
                              total_amount INT NOT NULL DEFAULT 0,
                              paid_amount INT NOT NULL DEFAULT 0,
                              cancelled_at DATETIME DEFAULT NULL,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              INDEX idx_res_guest (guest_id),
                              INDEX idx_res_status (status),
                              CONSTRAINT fk_res_guest
                                  FOREIGN KEY (guest_id) REFERENCES users(user_id) ON DELETE RESTRICT,
                              CONSTRAINT fk_res_hotel
                                  FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
                              CONSTRAINT fk_res_approved_by
                                  FOREIGN KEY (approved_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE reservation_items (
                                   reservation_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   reservation_id BIGINT NOT NULL,
                                   room_id BIGINT NOT NULL,
                                   room_type_id BIGINT NOT NULL,
                                   rate_per_night INT NOT NULL DEFAULT 0,
                                   nights INT NOT NULL DEFAULT 1,
                                   amount INT NOT NULL DEFAULT 0,
                                   status ENUM('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'BOOKED',
                                   UNIQUE (reservation_id, room_id),
                                   INDEX idx_ri_res (reservation_id),
                                   INDEX idx_ri_room (room_id),
                                   CONSTRAINT fk_ri_res
                                       FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_ri_room
                                       FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT,
                                   CONSTRAINT fk_ri_rt
                                       FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE payments (
                          payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          reservation_id BIGINT DEFAULT NULL,
                          guest_id BIGINT NOT NULL,
                          amount INT NOT NULL,
                          method ENUM('CARD','CASH','ONLINE') NOT NULL DEFAULT 'ONLINE',
                          provider VARCHAR(100) DEFAULT NULL,
                          provider_trans_id VARCHAR(100) DEFAULT NULL,
                          status ENUM('PENDING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
                          paid_at DATETIME DEFAULT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          INDEX idx_pay_res (reservation_id),
                          INDEX idx_pay_guest (guest_id),
                          CONSTRAINT fk_pay_res
                              FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE SET NULL,
                          CONSTRAINT fk_pay_guest
                              FOREIGN KEY (guest_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE notifications (
                               notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               type ENUM(
                                   'RESERVATION_CREATED',
                                   'RESERVATION_CONFIRMED',
                                   'RESERVATION_CHECKIN',
                                   'RESERVATION_CHECKOUT',
                                   'RESERVATION_CANCELLED',
                                   'RESERVATION_REMINDER'
                                   ) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               content TEXT,
                               reference_id BIGINT DEFAULT NULL, -- reservation_id
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               INDEX idx_notification_user_read (user_id, is_read),
                               INDEX idx_notification_created (created_at),
                               CONSTRAINT fk_notification_user
                                   FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =========================================================
-- 4) ROOM AVAILABILITY LOG (history) / MAINTENANCE / CHARGES
-- =========================================================
CREATE TABLE room_availability_logs (
                                        log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        room_id BIGINT NOT NULL,
                                        old_status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL,
                                        new_status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL,
                                        reason VARCHAR(255) DEFAULT NULL,
                                        changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        changed_by BIGINT DEFAULT NULL,
                                        CONSTRAINT fk_ral_room
                                            FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
                                        CONSTRAINT fk_ral_user
                                            FOREIGN KEY (changed_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE reservation_charges (
                                     charge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     reservation_id BIGINT NOT NULL,
                                     description VARCHAR(255) DEFAULT NULL,
                                     amount INT NOT NULL DEFAULT 0,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_charge_res
                                         FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =========================================================
-- 5) TRIGGERS: cập nhật room_types.total_rooms / available_rooms khi rooms thay đổi
-- =========================================================
ALTER TABLE rooms ADD INDEX idx_rooms_type_status (room_type_id, status);

DROP TRIGGER IF EXISTS trg_rooms_ai;
DROP TRIGGER IF EXISTS trg_rooms_ad;
DROP TRIGGER IF EXISTS trg_rooms_au;

DELIMITER $$

CREATE TRIGGER trg_rooms_ai
    AFTER INSERT ON rooms
    FOR EACH ROW
BEGIN
    UPDATE room_types
    SET total_rooms     = total_rooms + CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END,
        available_rooms = available_rooms + CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
    WHERE room_type_id = NEW.room_type_id;
END$$

CREATE TRIGGER trg_rooms_ad
    AFTER DELETE ON rooms
    FOR EACH ROW
BEGIN
    UPDATE room_types
    SET total_rooms     = total_rooms - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END,
        available_rooms = available_rooms - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END
    WHERE room_type_id = OLD.room_type_id;
END$$

CREATE TRIGGER trg_rooms_au
    AFTER UPDATE ON rooms
    FOR EACH ROW
BEGIN
    IF OLD.room_type_id <> NEW.room_type_id THEN
        -- decrement old room_type
        UPDATE room_types
        SET total_rooms     = total_rooms - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END,
            available_rooms = available_rooms - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END
        WHERE room_type_id = OLD.room_type_id;

        -- increment new room_type
        UPDATE room_types
        SET total_rooms     = total_rooms + CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END,
            available_rooms = available_rooms + CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
        WHERE room_type_id = NEW.room_type_id;
    ELSE
        -- same room_type: adjust deltas
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
-- NOTES on reservation -> room state transitions
-- =========================================================
-- As with library schema, changing a reservation item (i.e. BOOKED -> CHECKED_IN)
-- typically requires additional checks (room is AVAILABLE etc). It's usually safer
-- to perform these checks and the room state update inside backend business logic
-- (transactional), rather than relying on DB triggers to change rooms on INSERT to
-- reservation_items. If you prefer triggers, you can add them but be mindful of
-- concurrency and error signaling. (In the original library schema the insert trigger
-- on borrow_items was removed and handled in backend.)

-- =========================================================
-- 6) Seed roles / sample hotels / floors / amenities
-- =========================================================
INSERT INTO roles(name, description) VALUES
                                         ('ADMIN', 'Quản trị hệ thống'),
                                         ('STAFF', 'Nhân viên khách sạn'),
                                         ('GUEST', 'Khách');

INSERT INTO hotels (name, address, phone, email) VALUES
    ('Demo Hotel Central', '123 Example St, City', '+84-123-456-789', 'info@demo-hotel.example');

INSERT INTO floors (hotel_id, code, name) VALUES
                                              (1, 'F1', 'Floor 1'),
                                              (1, 'F2', 'Floor 2');

INSERT INTO amenities (name, description) VALUES
                                              ('WiFi', 'Internet không dây'),
                                              ('Breakfast', 'Bữa sáng miễn phí'),
                                              ('Parking', 'Chỗ đỗ xe'),
                                              ('Pool', 'Hồ bơi');

INSERT INTO room_types (hotel_id, code, name, description, capacity, price_per_night)
VALUES
    (1, 'STD', 'Standard', 'Standard room, 1-2 guests', 2, 800000),
    (1, 'DLX', 'Deluxe', 'Deluxe room with view', 3, 1200000);

-- You can add rooms after creating room_types; triggers sẽ cập nhật tổng số phòng tự động.
