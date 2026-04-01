-- =========================================================
-- V1.0.2 - FULL SEED DATA FOR HOTEL_DB
-- Scope: Insert sample data for all business tables
-- =========================================================

USE hotel_db;

-- =========================================================
-- CLEANUP (idempotent seed): avoid duplicate key on re-run
-- WARNING: this clears sample data in listed tables.
-- =========================================================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE room_availability_logs;
TRUNCATE TABLE reviews;
TRUNCATE TABLE notifications;
TRUNCATE TABLE payments;
TRUNCATE TABLE reservation_charges;
TRUNCATE TABLE reservation_items;
TRUNCATE TABLE reservations;
TRUNCATE TABLE room_images;
TRUNCATE TABLE rooms;
TRUNCATE TABLE room_type_amenities;
TRUNCATE TABLE amenities;
TRUNCATE TABLE room_type_images;
TRUNCATE TABLE room_types;
TRUNCATE TABLE floors;
TRUNCATE TABLE hotel_images;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE auth_tokens;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE profiles;
TRUNCATE TABLE users;
TRUNCATE TABLE roles;
SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1) RBAC + USERS
-- =========================================================

INSERT INTO roles (role_id, name, description) VALUES
(1, 'ADMIN', 'Quan tri he thong - toan quyen'),
(2, 'STAFF', 'Nhan vien khach san - van hanh dat phong'),
(3, 'GUEST', 'Khach hang dat phong');

INSERT INTO users (user_id, username, email, password_hash, status, last_login_at) VALUES
(1, 'staff.lead', 'staff.lead@hotel.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', '2026-03-30 08:40:00'),
(2, 'staff.frontdesk', 'frontdesk@hotel.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', '2026-03-30 08:15:00'),
(3, 'alice.nguyen', 'alice@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', '2026-03-29 22:10:00'),
(4, 'bob.tran', 'bob@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', '2026-03-29 20:30:00'),
(5, 'charlie.le', 'charlie@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PENDING', NULL),
(6, 'david.pham', 'david@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'BANNED', '2026-03-20 13:00:00');

# INSERT INTO profiles (profile_id, user_id, full_name, phone, avatar_url, address, dob, gender, national_id, nationality) VALUES
# (1, 1, 'Le Minh Quan', '0900000002', NULL, 'Ho Chi Minh', '1994-03-10', 'MALE', '001094000002', 'Vietnam'),
# (2, 2, 'Tran Thu Ha', '0900000003', NULL, 'Da Nang', '1996-11-21', 'FEMALE', '001096000003', 'Vietnam'),
# (3, 3, 'Alice Nguyen', '0900000004', NULL, 'Can Tho', '1998-07-17', 'FEMALE', '001098000004', 'Vietnam'),
# (4, 4, 'Bob Tran', '0900000005', NULL, 'Hai Phong', '1992-08-09', 'MALE', '001092000005', 'Vietnam'),
# (5, 5, 'Charlie Le', '0900000006', NULL, 'Hue', '2000-12-02', 'MALE', '001100000006', 'Vietnam'),
# (6, 6, 'David Pham', '0900000007', NULL, 'Nha Trang', '1988-05-14', 'MALE', '001088000007', 'Vietnam');

# INSERT INTO user_roles (user_id, role_id) VALUES
# (1, 2),
# (2, 2),
# (3, 3),
# (4, 3),
# (5, 3),
# (6, 3);

# INSERT INTO auth_tokens (token_id, user_id, purpose, token_hash, expires_at, used_at, otp_fail_count) VALUES
# (1, 5, 'VERIFY_EMAIL', 'seed_verify_token_hash_charlie', '2026-04-01 12:00:00', NULL, 0),
# (2, 3, 'RESET_PASSWORD', 'seed_reset_token_hash_alice', '2026-03-31 23:59:59', '2026-03-30 10:00:00', 0);
#
# INSERT INTO refresh_tokens (rt_id, user_id, token_hash, expires_at, revoked_at, user_agent, ip_address) VALUES
# (1, 1, 'seed_refresh_hash_staff_1', '2026-04-12 08:30:00', NULL, 'Chrome/124', '127.0.0.1'),
# (2, 3, 'seed_refresh_hash_alice_1', '2026-04-10 20:00:00', NULL, 'Edge/124', '127.0.0.1');

-- =========================================================
-- 2) HOTEL STRUCTURE + ROOM DOMAIN
-- =========================================================

INSERT INTO hotel_images (image_id, url, type, sort_order, is_primary, caption) VALUES
(1, 'https://images.example.com/hotel/exterior-1.jpg', 'EXTERIOR', 1, TRUE, 'Mat tien khach san'),
(2, 'https://images.example.com/hotel/lobby-1.jpg', 'LOBBY', 2, FALSE, 'Khu vuc sanh'),
(3, 'https://images.example.com/hotel/pool-1.jpg', 'POOL', 3, FALSE, 'Ho boi ngoai troi'),
(4, 'https://images.example.com/hotel/restaurant-1.jpg', 'RESTAURANT', 4, FALSE, 'Nha hang buffet'),
(5, 'https://images.example.com/hotel/facility-gym.jpg', 'FACILITY', 5, FALSE, 'Phong gym');

INSERT INTO floors (floor_id, code, name, floor_order) VALUES
(1, 'B1', 'Tang ham B1', -1),
(2, 'G', 'Tang tret', 0),
(3, 'F1', 'Tang 1', 1),
(4, 'F2', 'Tang 2', 2),
(5, 'F3', 'Tang 3', 3);

INSERT INTO room_types (
    room_type_id, code, name, description, capacity, max_adults, max_children,
    price_per_night, weekend_price, extra_bed_price, bed_type, bed_count, room_size, status
) VALUES
(1, 'STD', 'Standard', 'Phong tieu chuan, view thanh pho', 2, 2, 1, 800000, 900000, 200000, 'DOUBLE', 1, 25.00, 'ACTIVE'),
(2, 'SUP', 'Superior', 'Phong superior rong rai', 2, 2, 1, 1000000, 1150000, 250000, 'DOUBLE', 1, 30.00, 'ACTIVE'),
(3, 'DLX', 'Deluxe', 'Phong deluxe cao cap', 3, 2, 2, 1500000, 1700000, 300000, 'KING', 1, 35.00, 'ACTIVE'),
(4, 'STE', 'Suite', 'Phong suite premium', 4, 3, 2, 2500000, 2800000, 400000, 'KING', 1, 50.00, 'ACTIVE'),
(5, 'FAM', 'Family', 'Phong cho gia dinh', 5, 4, 2, 1800000, 2100000, 350000, 'QUEEN', 2, 42.00, 'ACTIVE');

INSERT INTO room_type_images (image_id, room_type_id, url, sort_order, is_primary, caption) VALUES
(1, 1, 'https://images.example.com/room-types/std-1.jpg', 1, TRUE, 'Standard main'),
(2, 1, 'https://images.example.com/room-types/std-2.jpg', 2, FALSE, 'Standard bathroom'),
(3, 2, 'https://images.example.com/room-types/sup-1.jpg', 1, TRUE, 'Superior main'),
(4, 2, 'https://images.example.com/room-types/sup-2.jpg', 2, FALSE, 'Superior corner'),
(5, 3, 'https://images.example.com/room-types/dlx-1.jpg', 1, TRUE, 'Deluxe main'),
(6, 3, 'https://images.example.com/room-types/dlx-2.jpg', 2, FALSE, 'Deluxe bathroom'),
(7, 4, 'https://images.example.com/room-types/ste-1.jpg', 1, TRUE, 'Suite living room'),
(8, 4, 'https://images.example.com/room-types/ste-2.jpg', 2, FALSE, 'Suite bedroom'),
(9, 5, 'https://images.example.com/room-types/fam-1.jpg', 1, TRUE, 'Family main'),
(10, 5, 'https://images.example.com/room-types/fam-2.jpg', 2, FALSE, 'Family twin beds');

INSERT INTO amenities (amenity_id, name, description, icon, category) VALUES
(1, 'WiFi mien phi', 'Internet khong day toc do cao', 'wifi', 'ROOM'),
(2, 'Dieu hoa', 'May lanh 2 chieu', 'snowflake', 'ROOM'),
(3, 'TV man hinh phang', 'Smart TV 43 inch', 'tv', 'ENTERTAINMENT'),
(4, 'Minibar', 'Tu lanh mini voi do uong', 'glass-martini', 'ROOM'),
(5, 'Ket sat', 'Ket an toan trong phong', 'lock', 'ROOM'),
(6, 'Bon tam', 'Bon tam nam', 'bath', 'BATHROOM'),
(7, 'Voi sen', 'Voi sen rieng', 'shower', 'BATHROOM'),
(8, 'Bua sang', 'Bua sang buffet mien phi', 'utensils', 'FOOD'),
(9, 'Ho boi', 'Ho boi ngoai troi', 'swimming-pool', 'FACILITY'),
(10, 'Gym', 'Phong tap the duc', 'dumbbell', 'FACILITY'),
(11, 'Spa', 'Dich vu spa va massage', 'spa', 'SERVICE'),
(12, 'Do xe', 'Bai do xe mien phi', 'parking', 'SERVICE'),
(13, 'Room service 24/7', 'Dich vu phong 24 gio', 'concierge-bell', 'SERVICE'),
(14, 'May say toc', 'May say toc trong phong tam', 'wind', 'BATHROOM');

INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 7),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 7), (2, 14),
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6), (3, 7), (3, 8), (3, 14),
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 10), (4, 14),
(5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 7), (5, 8), (5, 12), (5, 14);

INSERT INTO rooms (room_id, room_number, room_type_id, floor_id, status, note) VALUES
(1, '101', 1, 3, 'AVAILABLE', NULL),
(2, '102', 1, 3, 'AVAILABLE', NULL),
(3, '103', 1, 3, 'OCCUPIED', NULL),
(4, '104', 1, 3, 'MAINTENANCE', 'Kiem tra dieu hoa'),
(5, '105', 1, 3, 'AVAILABLE', NULL),
(6, '201', 2, 4, 'AVAILABLE', NULL),
(7, '202', 2, 4, 'HELD', 'Giu cho booking online'),
(8, '203', 2, 4, 'AVAILABLE', NULL),
(9, '204', 2, 4, 'OCCUPIED', NULL),
(10, '205', 2, 4, 'AVAILABLE', NULL),
(11, '301', 3, 5, 'AVAILABLE', NULL),
(12, '302', 3, 5, 'OCCUPIED', NULL),
(13, '303', 3, 5, 'AVAILABLE', NULL),
(14, '304', 3, 5, 'AVAILABLE', NULL),
(15, '305', 3, 5, 'REMOVED', 'Ngung su dung de cai tao'),
(16, '401', 4, 5, 'AVAILABLE', NULL),
(17, '402', 4, 5, 'AVAILABLE', NULL),
(18, '403', 4, 5, 'AVAILABLE', NULL),
(19, 'G01', 5, 2, 'AVAILABLE', NULL),
(20, 'G02', 5, 2, 'AVAILABLE', NULL);

INSERT INTO room_images (image_id, room_id, url, caption, is_primary, sort_order) VALUES
(1, 1, 'https://images.example.com/rooms/101-1.jpg', 'Phong 101 - goc 1', TRUE, 1),
(2, 1, 'https://images.example.com/rooms/101-2.jpg', 'Phong 101 - phong tam', FALSE, 2),
(3, 7, 'https://images.example.com/rooms/202-1.jpg', 'Phong 202 - tong quan', TRUE, 1),
(4, 11, 'https://images.example.com/rooms/301-1.jpg', 'Phong 301 - tong quan', TRUE, 1),
(5, 12, 'https://images.example.com/rooms/302-1.jpg', 'Phong 302 - tong quan', TRUE, 1),
(6, 16, 'https://images.example.com/rooms/401-1.jpg', 'Phong 401 - suite', TRUE, 1),
(7, 19, 'https://images.example.com/rooms/G01-1.jpg', 'Phong G01 - family', TRUE, 1);

-- =========================================================
-- 3) RESERVATIONS + PAYMENTS + REVIEWS + LOGS
-- =========================================================

INSERT INTO reservations (
    reservation_id, reservation_code, guest_id, status, check_in_date, check_out_date, nights_count,
    adult_count, child_count, total_amount, paid_amount, special_requests,
    confirmed_by, confirmed_at, checked_in_at, checked_out_at,
    cancelled_by, cancelled_at, cancel_reason
) VALUES
(1, 'R20260301001', 3, 'CHECKED_OUT', '2026-03-01', '2026-03-03', 2, 2, 0, 3000000, 3000000, 'Can phong yên tinh', 1, '2026-02-25 10:00:00', '2026-03-01 14:10:00', '2026-03-03 11:45:00', NULL, NULL, NULL),
(2, 'R20260305001', 4, 'CONFIRMED', '2026-04-05', '2026-04-08', 3, 2, 1, 3450000, 1000000, 'Them 1 goi bua sang', 2, '2026-03-29 09:30:00', NULL, NULL, NULL, NULL, NULL),
(3, 'R20260308001', 3, 'CANCELLED', '2026-03-10', '2026-03-11', 1, 1, 0, 900000, 0, NULL, 1, '2026-03-08 12:00:00', NULL, NULL, 3, '2026-03-09 09:00:00', 'Doi lich cong tac'),
(4, 'R20260312001', 4, 'CHECKED_IN', '2026-03-30', '2026-04-01', 2, 2, 0, 3400000, 3400000, 'Check-in som neu co the', 1, '2026-03-25 16:30:00', '2026-03-30 13:40:00', NULL, NULL, NULL, NULL),
(5, 'R20260315001', 3, 'PENDING', '2026-04-20', '2026-04-22', 2, 2, 2, 3600000, 0, 'Can them khan em be', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(6, 'R20260318001', 6, 'CHECKED_OUT', '2026-03-18', '2026-03-19', 1, 2, 0, 2800000, 2800000, NULL, 2, '2026-03-17 08:15:00', '2026-03-18 14:25:00', '2026-03-19 11:00:00', NULL, NULL, NULL);

INSERT INTO reservation_items (
    reservation_item_id, reservation_id, room_id, room_type_id, rate_per_night, nights, amount, status
) VALUES
(1, 1, 11, 3, 1500000, 2, 3000000, 'CHECKED_OUT'),
(2, 2, 7, 2, 1150000, 3, 3450000, 'BOOKED'),
(3, 3, 2, 1, 900000, 1, 900000, 'CANCELLED'),
(4, 4, 12, 3, 1700000, 2, 3400000, 'CHECKED_IN'),
(5, 5, 19, 5, 1800000, 2, 3600000, 'BOOKED'),
(6, 6, 16, 4, 2800000, 1, 2800000, 'CHECKED_OUT');

INSERT INTO reservation_charges (
    charge_id, reservation_id, charge_type, description, quantity, unit_price, amount, created_by
) VALUES
(1, 1, 'MINIBAR', 'Su dung nuoc ngot minibar', 2, 35000, 70000, 1),
(2, 1, 'LAUNDRY', 'Dich vu giat ui', 1, 120000, 120000, 1),
(3, 4, 'EXTRA_BED', 'Them giuong phu', 1, 300000, 300000, 2);

INSERT INTO payments (
    payment_id, reservation_id, guest_id, amount, method, provider, provider_trans_id, status, refund_amount, paid_at, note
) VALUES
(1, 1, 3, 3000000, 'ONLINE', 'ZALOPAY', 'ZP-TXN-20260301-0001', 'COMPLETED', 0, '2026-02-25 10:05:00', 'Thanh toan du 100%'),
(2, 2, 4, 1000000, 'ONLINE', 'ZALOPAY', 'ZP-TXN-20260329-0002', 'COMPLETED', 0, '2026-03-29 09:31:00', 'Dat coc truoc'),
(3, 3, 3, 900000, 'ONLINE', 'ZALOPAY', 'ZP-TXN-20260308-0003', 'REFUNDED', 900000, '2026-03-08 12:10:00', 'Hoan tien do huy som'),
(4, 4, 4, 3400000, 'CARD', 'POS', 'POS-20260330-0401', 'COMPLETED', 0, '2026-03-30 13:42:00', 'Thanh toan tai quay'),
(5, 5, 3, 3600000, 'ONLINE', 'ZALOPAY', 'ZP-TXN-20260331-0005', 'PENDING', 0, NULL, 'Cho xac nhan giao dich'),
(6, 6, 6, 2800000, 'CASH', 'FRONTDESK', 'CASH-20260318-0006', 'COMPLETED', 0, '2026-03-18 14:30:00', 'Thanh toan tien mat');

INSERT INTO notifications (
    notification_id, user_id, type, title, content, reference_id, reference_type, is_read, read_at
) VALUES
(1, 3, 'RESERVATION_CREATED', 'Dat phong thanh cong', 'Don R20260301001 da duoc tao', 1, 'RESERVATION', TRUE, '2026-02-25 10:10:00'),
(2, 4, 'RESERVATION_CONFIRMED', 'Don da duoc xac nhan', 'Don R20260305001 da duoc nhan vien xac nhan', 2, 'RESERVATION', FALSE, NULL),
(3, 3, 'RESERVATION_CANCELLED', 'Don da huy', 'Don R20260308001 da duoc huy theo yeu cau', 3, 'RESERVATION', TRUE, '2026-03-09 09:05:00'),
(4, 4, 'RESERVATION_CHECKIN', 'Da nhan phong', 'Ban da check-in cho don R20260312001', 4, 'RESERVATION', FALSE, NULL),
(5, 3, 'PAYMENT_FAILED', 'Thanh toan dang cho xu ly', 'Thanh toan don R20260315001 chua hoan tat', 5, 'PAYMENT', FALSE, NULL),
(6, 6, 'PAYMENT_SUCCESS', 'Thanh toan thanh cong', 'Da nhan thanh toan don R20260318001', 6, 'PAYMENT', TRUE, '2026-03-18 15:00:00');

INSERT INTO reviews (
    review_id, reservation_id, guest_id, rating, title, content,
    cleanliness_rating, service_rating, location_rating, facilities_rating, value_rating,
    is_verified, is_visible, reply, replied_at, replied_by
) VALUES
(1, 1, 3, 5, 'Trai nghiem rat tot', 'Phong sach se, nhan vien nhiet tinh va vi tri thuan tien.', 5, 5, 5, 4, 5, TRUE, TRUE,
 'Cam on ban da danh gia. Hen gap lai!', '2026-03-04 09:00:00', 1),
(2, 6, 6, 4, 'On dinh', 'Phong dep, check-in nhanh.', 4, 4, 4, 4, 4, TRUE, TRUE,
 NULL, NULL, NULL);

INSERT INTO room_availability_logs (
    log_id, room_id, old_status, new_status, reason, reservation_id, changed_at, changed_by
) VALUES
(1, 11, 'AVAILABLE', 'OCCUPIED', 'Khach check-in', 1, '2026-03-01 14:10:00', 1),
(2, 11, 'OCCUPIED', 'AVAILABLE', 'Khach check-out', 1, '2026-03-03 11:45:00', 1),
(3, 2, 'AVAILABLE', 'HELD', 'Giu phong truoc khi xac nhan', 3, '2026-03-08 11:30:00', 2),
(4, 2, 'HELD', 'AVAILABLE', 'Huy don dat phong', 3, '2026-03-09 09:00:00', 2),
(5, 12, 'AVAILABLE', 'OCCUPIED', 'Khach check-in', 4, '2026-03-30 13:40:00', 1),
(6, 16, 'AVAILABLE', 'OCCUPIED', 'Khach check-in', 6, '2026-03-18 14:25:00', 2),
(7, 16, 'OCCUPIED', 'AVAILABLE', 'Khach check-out', 6, '2026-03-19 11:00:00', 2),
(8, 4, 'AVAILABLE', 'MAINTENANCE', 'Bao tri dieu hoa', NULL, '2026-03-28 08:00:00', 1);

-- =========================================================
-- END SEED DATA
-- =========================================================

