-- Refactor schema to single-hotel model.
-- Remove hotel dimension columns and hotel tables.

-- Important: run this migration only when your data has one hotel tenant.

-- 1) Drop foreign keys to hotels
ALTER TABLE hotel_images DROP FOREIGN KEY fk_hi_hotel;
ALTER TABLE floors DROP FOREIGN KEY fk_floor_hotel;
ALTER TABLE room_types DROP FOREIGN KEY fk_rt_hotel;
ALTER TABLE rooms DROP FOREIGN KEY fk_room_hotel;
ALTER TABLE reservations DROP FOREIGN KEY fk_res_hotel;

-- 2) Drop indexes/unique constraints using hotel_id
ALTER TABLE floors DROP INDEX uq_floor_code_hotel;
ALTER TABLE room_types DROP INDEX uq_room_type_code_hotel;
ALTER TABLE rooms DROP INDEX uq_room_number_hotel;
ALTER TABLE reservations DROP INDEX idx_reservation_hotel_dates;

-- 3) Remove hotel_id columns
ALTER TABLE hotel_images DROP COLUMN hotel_id;
ALTER TABLE floors DROP COLUMN hotel_id;
ALTER TABLE room_types DROP COLUMN hotel_id;
ALTER TABLE rooms DROP COLUMN hotel_id;
ALTER TABLE reservations DROP COLUMN hotel_id;

-- 4) Recreate unique/index constraints without hotel dimension
ALTER TABLE floors ADD CONSTRAINT uq_floor_code UNIQUE (code);
ALTER TABLE room_types ADD CONSTRAINT uq_room_type_code UNIQUE (code);
ALTER TABLE rooms ADD CONSTRAINT uq_room_number UNIQUE (room_number);
ALTER TABLE reservations ADD INDEX idx_reservation_dates (check_in_date, check_out_date);

-- 5) Remove now-obsolete hotel tables
DROP TABLE hotel_images;
DROP TABLE hotels;


