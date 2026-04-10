-- =========================================================
-- SAMPLE SEED DATA: room_types, rooms, images, amenities
-- Idempotent: can run multiple times without creating duplicates.
-- Compatible with schema in V1.0.1__hotel_db.sql
-- =========================================================

USE hotel_db;

START TRANSACTION;

-- ---------------------------------------------------------
-- 1) ROOM TYPES
-- ---------------------------------------------------------
INSERT INTO room_types (
    code, name, description, capacity, max_adults, max_children,
    price_per_night, weekend_price, extra_bed_price,
    bed_type, bed_count, room_size, status
)
SELECT * FROM (
    SELECT 'STD' AS code, 'Standard' AS name, 'Phong tieu chuan, toi gian va day du tien nghi' AS description,
           2 AS capacity, 2 AS max_adults, 1 AS max_children,
           800000 AS price_per_night, 900000 AS weekend_price, 150000 AS extra_bed_price,
           'DOUBLE' AS bed_type, 1 AS bed_count, 24.00 AS room_size, 'ACTIVE' AS status
    UNION ALL
    SELECT 'SUP', 'Superior', 'Phong rong hon, phu hop cap doi',
           3, 2, 1,
           1100000, 1250000, 200000,
           'QUEEN', 1, 30.00, 'ACTIVE'
    UNION ALL
    SELECT 'DLX', 'Deluxe', 'Phong cao cap co view dep',
           4, 3, 2,
           1600000, 1850000, 250000,
           'KING', 1, 36.50, 'ACTIVE'
    UNION ALL
    SELECT 'STE', 'Suite', 'Phong suite cao cap cho gia dinh hoac nhom nho',
           5, 4, 2,
           2600000, 3000000, 350000,
           'KING', 2, 52.00, 'ACTIVE'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM room_types rt WHERE rt.code = seed.code
);

-- ---------------------------------------------------------
-- 2) AMENITIES
-- ---------------------------------------------------------
INSERT INTO amenities (name, description, icon, category)
SELECT * FROM (
    SELECT 'WiFi mien phi' AS name, 'Internet toc do cao khong gioi han' AS description, 'wifi' AS icon, 'ROOM' AS category
    UNION ALL
    SELECT 'Dieu hoa', 'May lanh 2 chieu', 'snowflake', 'ROOM'
    UNION ALL
    SELECT 'Smart TV', 'TV man hinh phang ket noi Internet', 'tv', 'ENTERTAINMENT'
    UNION ALL
    SELECT 'Minibar', 'Do uong va snack trong phong', 'glass-martini', 'FOOD'
    UNION ALL
    SELECT 'Bon tam', 'Bon tam rieng trong phong tam', 'bath', 'BATHROOM'
    UNION ALL
    SELECT 'May say toc', 'May say toc cao cap', 'wind', 'BATHROOM'
    UNION ALL
    SELECT 'An sang buffet', 'Bao gom buffet sang tai nha hang', 'utensils', 'FOOD'
    UNION ALL
    SELECT 'Dich vu phong 24/7', 'Ho tro room service moi luc', 'concierge-bell', 'SERVICE'
    UNION ALL
    SELECT 'Ho boi', 'Ho boi ngoai troi cua khach san', 'swimming-pool', 'FACILITY'
    UNION ALL
    SELECT 'Phong gym', 'Khu vuc tap luyen day du thiet bi', 'dumbbell', 'FACILITY'
) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM amenities a WHERE a.name = seed.name
);

-- ---------------------------------------------------------
-- 3) ROOM TYPE <-> AMENITIES (many-to-many)
-- ---------------------------------------------------------
INSERT INTO room_type_amenities (room_type_id, amenity_id)
SELECT rt.room_type_id, am.amenity_id
FROM (
    SELECT 'STD' AS room_code, 'WiFi mien phi' AS amenity_name
    UNION ALL SELECT 'STD', 'Dieu hoa'
    UNION ALL SELECT 'STD', 'Smart TV'

    UNION ALL SELECT 'SUP', 'WiFi mien phi'
    UNION ALL SELECT 'SUP', 'Dieu hoa'
    UNION ALL SELECT 'SUP', 'Smart TV'
    UNION ALL SELECT 'SUP', 'May say toc'

    UNION ALL SELECT 'DLX', 'WiFi mien phi'
    UNION ALL SELECT 'DLX', 'Dieu hoa'
    UNION ALL SELECT 'DLX', 'Smart TV'
    UNION ALL SELECT 'DLX', 'Minibar'
    UNION ALL SELECT 'DLX', 'Bon tam'
    UNION ALL SELECT 'DLX', 'An sang buffet'

    UNION ALL SELECT 'STE', 'WiFi mien phi'
    UNION ALL SELECT 'STE', 'Dieu hoa'
    UNION ALL SELECT 'STE', 'Smart TV'
    UNION ALL SELECT 'STE', 'Minibar'
    UNION ALL SELECT 'STE', 'Bon tam'
    UNION ALL SELECT 'STE', 'May say toc'
    UNION ALL SELECT 'STE', 'An sang buffet'
    UNION ALL SELECT 'STE', 'Dich vu phong 24/7'
    UNION ALL SELECT 'STE', 'Ho boi'
    UNION ALL SELECT 'STE', 'Phong gym'
) map
JOIN room_types rt ON rt.code = map.room_code
JOIN amenities am ON am.name = map.amenity_name
LEFT JOIN room_type_amenities rta
       ON rta.room_type_id = rt.room_type_id AND rta.amenity_id = am.amenity_id
WHERE rta.room_type_id IS NULL;

-- ---------------------------------------------------------
-- 4) ROOM TYPE IMAGES
-- ---------------------------------------------------------
INSERT INTO room_type_images (room_type_id, url, sort_order, is_primary, caption)
SELECT rt.room_type_id, seed.url, seed.sort_order, seed.is_primary, seed.caption
FROM (
    SELECT 'STD' AS room_code, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=1200' AS url, 0 AS sort_order, TRUE AS is_primary, 'Standard - main' AS caption
    UNION ALL
    SELECT 'STD', 'https://images.unsplash.com/photo-1616594039964-3f3b3c2b2e6a?w=1200', 1, FALSE, 'Standard - bathroom'
    UNION ALL
    SELECT 'SUP', 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=1200', 0, TRUE, 'Superior - main'
    UNION ALL
    SELECT 'SUP', 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=1200', 1, FALSE, 'Superior - workspace'
    UNION ALL
    SELECT 'DLX', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=1200', 0, TRUE, 'Deluxe - main'
    UNION ALL
    SELECT 'DLX', 'https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=1200', 1, FALSE, 'Deluxe - bathroom'
    UNION ALL
    SELECT 'STE', 'https://images.unsplash.com/photo-1591088398332-8a7791972843?w=1200', 0, TRUE, 'Suite - living area'
    UNION ALL
    SELECT 'STE', 'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=1200', 1, FALSE, 'Suite - bedroom'
) seed
JOIN room_types rt ON rt.code = seed.room_code
LEFT JOIN room_type_images rti
       ON rti.room_type_id = rt.room_type_id AND rti.url = seed.url
WHERE rti.image_id IS NULL;

-- ---------------------------------------------------------
-- 5) ROOMS
-- ---------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_room_seed;
CREATE TEMPORARY TABLE tmp_room_seed (
    room_number VARCHAR(50) NOT NULL,
    room_type_id BIGINT NOT NULL,
    floor_id INT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(500) NULL,
    PRIMARY KEY (room_number)
);

INSERT INTO tmp_room_seed (room_number, room_type_id, floor_id, status, note)
SELECT seed.room_number,
       rt.room_type_id,
       f.floor_id,
       seed.status,
       seed.note
FROM (
    SELECT '101' AS room_number, 'STD' AS room_code, 'F1' AS floor_code, 'AVAILABLE' AS status, 'Gan cua so' AS note
    UNION ALL SELECT '102', 'STD', 'F1', 'AVAILABLE', 'Yen tinh'
    UNION ALL SELECT '103', 'STD', 'F1', 'AVAILABLE', 'Gan thang may'
    UNION ALL SELECT '104', 'STD', 'F1', 'AVAILABLE', 'Dat truoc ngan han'
    UNION ALL SELECT '105', 'STD', 'F1', 'AVAILABLE', 'Khong hut thuoc'
    UNION ALL SELECT '106', 'STD', 'F1', 'MAINTENANCE', 'Bao tri den phong'
    UNION ALL SELECT '201', 'SUP', 'F2', 'AVAILABLE', 'View thanh pho'
    UNION ALL SELECT '202', 'SUP', 'F2', 'AVAILABLE', 'Dang giu cho'
    UNION ALL SELECT '203', 'SUP', 'F2', 'AVAILABLE', 'Gan khu pantry'
    UNION ALL SELECT '204', 'SUP', 'F2', 'AVAILABLE', 'View ho boi'
    UNION ALL SELECT '205', 'SUP', 'F2', 'OCCUPIED', 'Khach luu tru dai ngay'
    UNION ALL SELECT '206', 'SUP', 'F2', 'AVAILABLE', 'Phong goc yên tinh'
    UNION ALL SELECT '301', 'DLX', 'F3', 'AVAILABLE', 'View dep'
    UNION ALL SELECT '302', 'DLX', 'F3', 'MAINTENANCE', 'Bao tri dieu hoa'
    UNION ALL SELECT '303', 'DLX', 'F3', 'AVAILABLE', 'Gan khu lounge'
    UNION ALL SELECT '304', 'DLX', 'F3', 'AVAILABLE', 'Cho thanh toan VNPay'
    UNION ALL SELECT '305', 'DLX', 'F3', 'AVAILABLE', 'Khong hut thuoc'
    UNION ALL SELECT '306', 'DLX', 'F3', 'OCCUPIED', 'Doan khach gia dinh'
    UNION ALL SELECT '401', 'STE', 'F3', 'AVAILABLE', 'Canh goc, rong'
    UNION ALL SELECT '402', 'STE', 'F3', 'OCCUPIED', 'Khach dang luu tru'
    UNION ALL SELECT '403', 'STE', 'F3', 'AVAILABLE', 'View toan canh thanh pho'
    UNION ALL SELECT '404', 'STE', 'F3', 'AVAILABLE', 'Phong tong thong mini'
    UNION ALL SELECT '405', 'STE', 'F3', 'AVAILABLE', 'Dat cho VIP'
    UNION ALL SELECT '406', 'STE', 'F3', 'MAINTENANCE', 'Bao tri phong tam'
) seed
JOIN room_types rt ON rt.code = seed.room_code
LEFT JOIN floors f ON f.code = seed.floor_code;

INSERT INTO rooms (room_number, room_type_id, floor_id, status, note)
SELECT seed.room_number,
       seed.room_type_id,
       seed.floor_id,
       seed.status,
       seed.note
FROM tmp_room_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM rooms r WHERE r.room_number = seed.room_number
);

DROP TEMPORARY TABLE IF EXISTS tmp_room_seed;

-- ---------------------------------------------------------
-- 6) ROOM IMAGES (physical room)
-- ---------------------------------------------------------
INSERT INTO room_images (room_id, url, caption, is_primary, sort_order)
SELECT r.room_id, seed.url, seed.caption, seed.is_primary, seed.sort_order
FROM (
    SELECT '101' AS room_number, 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=1200' AS url, 'Room 101 - main' AS caption, TRUE AS is_primary, 0 AS sort_order
    UNION ALL
    SELECT '101', 'https://images.unsplash.com/photo-1616594039964-3f3b3c2b2e6a?w=1200', 'Room 101 - detail', FALSE, 1
    UNION ALL
    SELECT '201', 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=1200', 'Room 201 - main', TRUE, 0
    UNION ALL
    SELECT '301', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=1200', 'Room 301 - main', TRUE, 0
    UNION ALL
    SELECT '401', 'https://images.unsplash.com/photo-1591088398332-8a7791972843?w=1200', 'Room 401 - main', TRUE, 0
) seed
JOIN rooms r ON r.room_number = seed.room_number
LEFT JOIN room_images ri
       ON ri.room_id = r.room_id AND ri.url = seed.url
WHERE ri.image_id IS NULL;

COMMIT;

-- End of sample seed script.

