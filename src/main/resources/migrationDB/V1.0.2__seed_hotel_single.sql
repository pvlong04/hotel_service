-- Seed data for a single-hotel setup (idempotent)
-- Safe to run multiple times.

# SET @hotel_email = 'info@demo-hotel.example';

INSERT INTO hotels (
    name,
    address,
    city,
    country,
    phone,
    email,
    website,
    description,
    star_rating,
    check_in_time,
    check_out_time,
    latitude,
    longitude,
    timezone,
    status
)
SELECT
    'Demo Hotel Central',
    '123 Example St, District 1',
    'Ho Chi Minh City',
    'Viet Nam',
    '+84-123-456-789',
    @hotel_email,
    'https://demo-hotel.example',
    'Modern city-center hotel for MVP booking flow.',
    4,
    '14:00:00',
    '12:00:00',
    10.77653000,
    106.70098000,
    'Asia/Ho_Chi_Minh',
    'ACTIVE'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM hotels h WHERE h.email = @hotel_email
);

SET @hotel_id = (
    SELECT h.hotel_id
    FROM hotels h
    WHERE h.email = @hotel_email
    LIMIT 1
);

INSERT INTO floors (hotel_id, code, name, floor_order)
SELECT @hotel_id, 'G', 'Ground Floor', 0
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM floors f WHERE f.hotel_id = @hotel_id AND f.code = 'G'
  );

INSERT INTO floors (hotel_id, code, name, floor_order)
SELECT @hotel_id, 'F1', 'Floor 1', 1
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM floors f WHERE f.hotel_id = @hotel_id AND f.code = 'F1'
  );

INSERT INTO floors (hotel_id, code, name, floor_order)
SELECT @hotel_id, 'F2', 'Floor 2', 2
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM floors f WHERE f.hotel_id = @hotel_id AND f.code = 'F2'
  );

INSERT INTO hotel_images (hotel_id, url, type, sort_order, is_primary, caption)
SELECT @hotel_id,
       'https://images.unsplash.com/photo-1566073771259-6a8506099945',
       'EXTERIOR',
       0,
       TRUE,
       'Hotel exterior'
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hotel_images hi
      WHERE hi.hotel_id = @hotel_id
        AND hi.url = 'https://images.unsplash.com/photo-1566073771259-6a8506099945'
  );

INSERT INTO hotel_images (hotel_id, url, type, sort_order, is_primary, caption)
SELECT @hotel_id,
       'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa',
       'LOBBY',
       1,
       FALSE,
       'Hotel lobby'
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hotel_images hi
      WHERE hi.hotel_id = @hotel_id
        AND hi.url = 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa'
  );

-- =========================================================
-- Seed amenities / room types / rooms for single-hotel MVP
-- =========================================================

INSERT IGNORE INTO amenities (name, description, icon, category)
VALUES
    ('WiFi mien phi', 'Internet khong day toc do cao', 'wifi', 'ROOM'),
    ('Dieu hoa', 'May lanh 2 chieu', 'snowflake', 'ROOM'),
    ('TV man hinh phang', 'Smart TV 43 inch', 'tv', 'ENTERTAINMENT'),
    ('Minibar', 'Tu lanh mini voi do uong', 'glass-martini', 'ROOM'),
    ('Ket sat', 'Ket an toan trong phong', 'lock', 'ROOM'),
    ('Bon tam', 'Bon tam nam', 'bath', 'BATHROOM'),
    ('Voi sen', 'Voi sen rieng', 'shower', 'BATHROOM'),
    ('May say toc', 'May say toc trong phong tam', 'wind', 'BATHROOM');

INSERT INTO room_types (
    hotel_id,
    code,
    name,
    description,
    capacity,
    max_adults,
    max_children,
    price_per_night,
    weekend_price,
    extra_bed_price,
    bed_type,
    bed_count,
    room_size,
    status
)
SELECT @hotel_id, 'STD', 'Standard', 'Phong tieu chuan, view thanh pho', 2, 2, 1, 800000, 900000, 200000, 'DOUBLE', 1, 25.00, 'ACTIVE'
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM room_types rt WHERE rt.hotel_id = @hotel_id AND rt.code = 'STD'
  );

INSERT INTO room_types (
    hotel_id,
    code,
    name,
    description,
    capacity,
    max_adults,
    max_children,
    price_per_night,
    weekend_price,
    extra_bed_price,
    bed_type,
    bed_count,
    room_size,
    status
)
SELECT @hotel_id, 'SUP', 'Superior', 'Phong superior rong rai hon', 2, 2, 1, 1000000, 1150000, 250000, 'DOUBLE', 1, 30.00, 'ACTIVE'
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM room_types rt WHERE rt.hotel_id = @hotel_id AND rt.code = 'SUP'
  );

INSERT INTO room_types (
    hotel_id,
    code,
    name,
    description,
    capacity,
    max_adults,
    max_children,
    price_per_night,
    weekend_price,
    extra_bed_price,
    bed_type,
    bed_count,
    room_size,
    status
)
SELECT @hotel_id, 'DLX', 'Deluxe', 'Phong deluxe voi view dep', 3, 2, 2, 1500000, 1700000, 300000, 'KING', 1, 35.00, 'ACTIVE'
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM room_types rt WHERE rt.hotel_id = @hotel_id AND rt.code = 'DLX'
  );

INSERT INTO room_types (
    hotel_id,
    code,
    name,
    description,
    capacity,
    max_adults,
    max_children,
    price_per_night,
    weekend_price,
    extra_bed_price,
    bed_type,
    bed_count,
    room_size,
    status
)
SELECT @hotel_id, 'STE', 'Suite', 'Phong suite cao cap', 4, 3, 2, 2500000, 2800000, 400000, 'KING', 1, 50.00, 'ACTIVE'
FROM DUAL
WHERE @hotel_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM room_types rt WHERE rt.hotel_id = @hotel_id AND rt.code = 'STE'
  );

INSERT INTO room_type_amenities (room_type_id, amenity_id)
SELECT rt.room_type_id, a.amenity_id
FROM room_types rt
JOIN amenities a ON a.name IN ('WiFi mien phi', 'Dieu hoa', 'TV man hinh phang', 'Voi sen')
WHERE rt.hotel_id = @hotel_id
  AND rt.code = 'STD'
  AND NOT EXISTS (
      SELECT 1
      FROM room_type_amenities rta
      WHERE rta.room_type_id = rt.room_type_id
        AND rta.amenity_id = a.amenity_id
  );

INSERT INTO room_type_amenities (room_type_id, amenity_id)
SELECT rt.room_type_id, a.amenity_id
FROM room_types rt
JOIN amenities a ON a.name IN ('WiFi mien phi', 'Dieu hoa', 'TV man hinh phang', 'Minibar', 'Ket sat', 'Voi sen', 'May say toc')
WHERE rt.hotel_id = @hotel_id
  AND rt.code = 'SUP'
  AND NOT EXISTS (
      SELECT 1
      FROM room_type_amenities rta
      WHERE rta.room_type_id = rt.room_type_id
        AND rta.amenity_id = a.amenity_id
  );

INSERT INTO room_type_amenities (room_type_id, amenity_id)
SELECT rt.room_type_id, a.amenity_id
FROM room_types rt
JOIN amenities a ON a.name IN ('WiFi mien phi', 'Dieu hoa', 'TV man hinh phang', 'Minibar', 'Ket sat', 'Bon tam', 'Voi sen', 'May say toc')
WHERE rt.hotel_id = @hotel_id
  AND rt.code IN ('DLX', 'STE')
  AND NOT EXISTS (
      SELECT 1
      FROM room_type_amenities rta
      WHERE rta.room_type_id = rt.room_type_id
        AND rta.amenity_id = a.amenity_id
  );

INSERT INTO rooms (hotel_id, room_number, room_type_id, floor_id, status, note)
SELECT data.hotel_id,
       data.room_number,
       rt.room_type_id,
       f.floor_id,
       data.status,
       data.note
FROM (
    SELECT @hotel_id AS hotel_id, '101' AS room_number, 'STD' AS room_type_code, 'F1' AS floor_code, 'AVAILABLE' AS status, NULL AS note
    UNION ALL SELECT @hotel_id, '102', 'STD', 'F1', 'AVAILABLE', NULL
    UNION ALL SELECT @hotel_id, '103', 'SUP', 'F1', 'AVAILABLE', NULL
    UNION ALL SELECT @hotel_id, '104', 'SUP', 'F1', 'MAINTENANCE', 'Bao tri dieu hoa'
    UNION ALL SELECT @hotel_id, '201', 'DLX', 'F2', 'AVAILABLE', NULL
    UNION ALL SELECT @hotel_id, '202', 'DLX', 'F2', 'AVAILABLE', NULL
    UNION ALL SELECT @hotel_id, '203', 'STE', 'F2', 'AVAILABLE', NULL
) data
JOIN room_types rt
    ON rt.hotel_id = data.hotel_id
   AND rt.code = data.room_type_code
LEFT JOIN floors f
    ON f.hotel_id = data.hotel_id
   AND f.code = data.floor_code
LEFT JOIN rooms r
    ON r.hotel_id = data.hotel_id
   AND r.room_number = data.room_number
WHERE data.hotel_id IS NOT NULL
  AND r.room_id IS NULL;

UPDATE room_types rt
LEFT JOIN (
    SELECT room_type_id,
           COUNT(*) AS total_rooms,
           SUM(CASE WHEN status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_rooms
    FROM rooms
    GROUP BY room_type_id
) stats ON stats.room_type_id = rt.room_type_id
SET rt.total_rooms = COALESCE(stats.total_rooms, 0),
    rt.available_rooms = COALESCE(stats.available_rooms, 0)
WHERE rt.hotel_id = @hotel_id;

