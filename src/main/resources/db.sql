-- Rebuild database for development
DROP DATABASE IF EXISTS hotel_db;
CREATE DATABASE hotel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_db;

SET sql_mode = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

-- =========================================================
-- 1) AUTH / USER / RBAC
-- =========================================================
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status ENUM('PENDING','ACTIVE','BANNED') NOT NULL DEFAULT 'PENDING',
    last_login_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE profiles (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) UNIQUE NULL,
    avatar_url VARCHAR(500) NULL,
    address VARCHAR(500) NULL,
    dob DATE NULL,
    gender ENUM('MALE','FEMALE','OTHER') NULL,
    national_id VARCHAR(50) NULL,
    nationality VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    name ENUM('ADMIN','STAFF','GUEST') NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE auth_tokens (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose ENUM('VERIFY_EMAIL','VERIFY_EMAIL_OTP','RESET_PASSWORD') NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    otp_fail_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auth_token_user_purpose (user_id, purpose),
    INDEX idx_auth_token_expires (expires_at),
    CONSTRAINT fk_auth_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    rt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    user_agent VARCHAR(255) NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rt_user (user_id),
    INDEX idx_rt_expires (expires_at),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 2) HOTEL / FLOOR / ROOM TYPE / ROOM / IMAGE / AMENITY
-- =========================================================
CREATE TABLE hotels (
    hotel_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    address VARCHAR(255) NULL,
    city VARCHAR(100) NULL,
    country VARCHAR(100) NULL,
    phone VARCHAR(64) NULL,
    email VARCHAR(150) NULL,
    website VARCHAR(255) NULL,
    star_rating INT NULL,
    check_in_time TIME NULL,
    check_out_time TIME NULL,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    timezone VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE hotel_images (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id INT NOT NULL,
    url VARCHAR(500) NOT NULL,
    caption VARCHAR(255) NULL,
    type ENUM('EXTERIOR','LOBBY','RESTAURANT','POOL','FACILITY','OTHER') NOT NULL DEFAULT 'OTHER',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hotel_image_hotel (hotel_id),
    CONSTRAINT fk_hotel_image_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE floors (
    floor_id INT AUTO_INCREMENT PRIMARY KEY,
    hotel_id INT NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_floor_code_hotel (hotel_id, code),
    CONSTRAINT fk_floor_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE amenities (
    amenity_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    icon VARCHAR(100) NULL,
    category ENUM('ROOM','BATHROOM','ENTERTAINMENT','FOOD','FACILITY','SERVICE') NOT NULL DEFAULT 'ROOM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE room_types (
    room_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id INT NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    capacity INT NOT NULL DEFAULT 2,
    max_adults INT NOT NULL DEFAULT 2,
    max_children INT NOT NULL DEFAULT 0,
    price_per_night BIGINT NOT NULL DEFAULT 0,
    weekend_price BIGINT NULL,
    extra_bed_price BIGINT NULL,
    bed_type ENUM('SINGLE','DOUBLE','TWIN','KING','QUEEN') NULL,
    bed_count INT NOT NULL DEFAULT 1,
    room_size DECIMAL(6,2) NULL,
    status ENUM('ACTIVE','INACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    total_rooms INT NOT NULL DEFAULT 0,
    available_rooms INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_room_type_code_hotel (hotel_id, code),
    INDEX idx_rt_hotel (hotel_id),
    CONSTRAINT fk_rt_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE room_type_amenities (
    room_type_id BIGINT NOT NULL,
    amenity_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_type_id, amenity_id),
    CONSTRAINT fk_rta_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE CASCADE,
    CONSTRAINT fk_rta_amenity FOREIGN KEY (amenity_id) REFERENCES amenities(amenity_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE room_type_images (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    caption VARCHAR(255) NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rti_room_type (room_type_id),
    CONSTRAINT fk_rti_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE rooms (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hotel_id INT NOT NULL,
    room_number VARCHAR(50) NOT NULL,
    room_type_id BIGINT NOT NULL,
    floor_id INT NULL,
    status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL DEFAULT 'AVAILABLE',
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_room_number_hotel (hotel_id, room_number),
    INDEX idx_room_hotel (hotel_id),
    INDEX idx_room_type (room_type_id),
    INDEX idx_room_status (status),
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
    CONSTRAINT fk_room_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_room_floor FOREIGN KEY (floor_id) REFERENCES floors(floor_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE room_images (
    image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    caption VARCHAR(255) NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_room_image_room (room_id),
    CONSTRAINT fk_room_image_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE room_availability_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    old_status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL,
    new_status ENUM('AVAILABLE','HELD','OCCUPIED','MAINTENANCE','REMOVED') NOT NULL,
    reason VARCHAR(255) NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by BIGINT NULL,
    CONSTRAINT fk_ral_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_ral_user FOREIGN KEY (changed_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =========================================================
-- 3) RESERVATION / PAYMENT / CHARGES / NOTIFICATION
-- =========================================================
CREATE TABLE reservations (
    reservation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_code VARCHAR(40) NOT NULL UNIQUE,
    guest_id BIGINT NOT NULL,
    hotel_id INT NOT NULL,
    status ENUM('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'PENDING',
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    nights_count INT NOT NULL DEFAULT 1,
    adult_count INT NOT NULL DEFAULT 1,
    child_count INT NOT NULL DEFAULT 0,
    special_requests TEXT NULL,
    total_amount BIGINT NOT NULL DEFAULT 0,
    paid_amount BIGINT NOT NULL DEFAULT 0,
    confirmed_by BIGINT NULL,
    confirmed_at DATETIME NULL,
    checked_in_at DATETIME NULL,
    checked_out_at DATETIME NULL,
    cancel_reason VARCHAR(500) NULL,
    cancelled_by BIGINT NULL,
    cancelled_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_reservation_guest (guest_id),
    INDEX idx_reservation_status (status),
    INDEX idx_reservation_hotel_dates (hotel_id, check_in_date, check_out_date),
    CONSTRAINT fk_res_guest FOREIGN KEY (guest_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_hotel FOREIGN KEY (hotel_id) REFERENCES hotels(hotel_id) ON DELETE CASCADE,
    CONSTRAINT fk_res_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_res_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE reservation_items (
    reservation_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    room_type_id BIGINT NOT NULL,
    rate_per_night BIGINT NOT NULL DEFAULT 0,
    nights INT NOT NULL DEFAULT 1,
    amount BIGINT NOT NULL DEFAULT 0,
    status ENUM('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED') NOT NULL DEFAULT 'BOOKED',
    checked_in_at DATETIME NULL,
    checked_out_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_reservation_room (reservation_id, room_id),
    INDEX idx_res_item_reservation (reservation_id),
    INDEX idx_res_item_room (room_id),
    CONSTRAINT fk_res_item_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_res_item_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT,
    CONSTRAINT fk_res_item_room_type FOREIGN KEY (room_type_id) REFERENCES room_types(room_type_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE reservation_charges (
    charge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    charge_type ENUM('EXTRA_BED','MINIBAR','ROOM_SERVICE','LAUNDRY','DAMAGE','LATE_CHECKOUT','CANCEL_FEE','OTHER') NOT NULL DEFAULT 'OTHER',
    description VARCHAR(500) NULL,
    amount BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_charge_reservation (reservation_id),
    CONSTRAINT fk_charge_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_charge_created_by FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    guest_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    method ENUM('CARD','CASH','BANK_TRANSFER','E_WALLET','ONLINE') NOT NULL,
    provider VARCHAR(100) NULL,
    provider_trans_id VARCHAR(150) NULL,
    status ENUM('PENDING','COMPLETED','FAILED','REFUNDED','PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
    note VARCHAR(500) NULL,
    paid_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payment_reservation (reservation_id),
    INDEX idx_payment_guest (guest_id),
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_guest FOREIGN KEY (guest_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM(
        'RESERVATION_CREATED','RESERVATION_CONFIRMED','RESERVATION_CHECKIN','RESERVATION_CHECKOUT',
        'RESERVATION_CANCELLED','RESERVATION_REMINDER','PAYMENT_SUCCESS','PAYMENT_FAILED',
        'REVIEW_REQUEST','PROMOTION','SYSTEM'
    ) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    reference_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_user_read (user_id, is_read),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 4) Trigger: keep room_types.total_rooms / available_rooms consistent
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
    SET total_rooms = total_rooms + CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END,
        available_rooms = available_rooms + CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
    WHERE room_type_id = NEW.room_type_id;
END$$

CREATE TRIGGER trg_rooms_ad
AFTER DELETE ON rooms
FOR EACH ROW
BEGIN
    UPDATE room_types
    SET total_rooms = total_rooms - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END,
        available_rooms = available_rooms - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END
    WHERE room_type_id = OLD.room_type_id;
END$$

CREATE TRIGGER trg_rooms_au
AFTER UPDATE ON rooms
FOR EACH ROW
BEGIN
    IF OLD.room_type_id <> NEW.room_type_id THEN
        UPDATE room_types
        SET total_rooms = total_rooms - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END,
            available_rooms = available_rooms - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END
        WHERE room_type_id = OLD.room_type_id;

        UPDATE room_types
        SET total_rooms = total_rooms + CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END,
            available_rooms = available_rooms + CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END
        WHERE room_type_id = NEW.room_type_id;
    ELSE
        UPDATE room_types
        SET total_rooms = total_rooms
            + (CASE WHEN NEW.status <> 'REMOVED' THEN 1 ELSE 0 END - CASE WHEN OLD.status <> 'REMOVED' THEN 1 ELSE 0 END),
            available_rooms = available_rooms
            + (CASE WHEN NEW.status = 'AVAILABLE' THEN 1 ELSE 0 END - CASE WHEN OLD.status = 'AVAILABLE' THEN 1 ELSE 0 END)
        WHERE room_type_id = NEW.room_type_id;
    END IF;
END$$

DELIMITER ;

-- =========================================================
-- 5) Minimal seeds
-- =========================================================
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Quan tri he thong'),
('STAFF', 'Nhan vien khach san'),
('GUEST', 'Khach hang');

INSERT INTO hotels (name, address, city, country, phone, email, status)
VALUES ('Demo Hotel Central', '123 Example St', 'Ho Chi Minh', 'Viet Nam', '+84-123-456-789', 'info@demo-hotel.example', 'ACTIVE');

INSERT INTO floors (hotel_id, code, name) VALUES
(1, 'F1', 'Tang 1'),
(1, 'F2', 'Tang 2');

INSERT INTO amenities (name, description, category) VALUES
('WiFi', 'Internet khong day', 'ROOM'),
('Breakfast', 'Bua sang mien phi', 'FOOD'),
('Parking', 'Cho do xe', 'FACILITY'),
('Pool', 'Ho boi', 'FACILITY');
