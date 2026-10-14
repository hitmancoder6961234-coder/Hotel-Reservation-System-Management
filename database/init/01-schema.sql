-- ============================================================================
--  StayEase - MySQL schema (migrated from the Prisma/SQLite schema)
--
--  Table and column names are unchanged from the original (Room, Booking,
--  bookingRef, guestName, ...). Type mapping:
--    SQLite String "YYYY-MM-DD"  ->  DATE          (checkIn, checkOut)
--    SQLite DateTime             ->  DATETIME(3)   (createdAt, stored in UTC)
--    SQLite Boolean              ->  TINYINT(1)    (available)
--    SQLite Int / String         ->  INT / VARCHAR / TEXT
--  Added safety nets (the application already enforces these rules):
--    CHECK (checkOut > checkIn), CHECK (guests >= 1 AND nights >= 1) and a
--    composite index that makes the overlap query fast.
--  Idempotent: safe to run more than once (CREATE TABLE IF NOT EXISTS).
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `Room` (
  `id`          INT          NOT NULL,                       -- 101, 102, 103 ...
  `name`        VARCHAR(100) NOT NULL,
  `description` TEXT         NOT NULL,
  `price`       INT          NOT NULL,                       -- per night, INR
  `capacity`    INT          NOT NULL,                       -- maximum guests
  `facilities`  VARCHAR(500) NOT NULL,                       -- comma separated: "AC, WiFi, TV"
  `features`    VARCHAR(1000) NOT NULL,                      -- comma separated room features
  `image`       VARCHAR(255) NOT NULL,                       -- e.g. /images/deluxe.jpg
  `available`   TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `Booking` (
  `id`         INT          NOT NULL AUTO_INCREMENT,
  `bookingRef` VARCHAR(32)  NOT NULL,                        -- STY-2026-001
  `guestName`  VARCHAR(255) NOT NULL,
  `email`      VARCHAR(254) NOT NULL,
  `mobile`     VARCHAR(32)  NOT NULL,
  `roomId`     INT          NOT NULL,
  `checkIn`    DATE         NOT NULL,
  `checkOut`   DATE         NOT NULL,
  `guests`     INT          NOT NULL,
  `nights`     INT          NOT NULL,
  `totalPrice` INT          NOT NULL,
  `status`     VARCHAR(20)  NOT NULL DEFAULT 'Confirmed',
  `createdAt`  DATETIME(3)  NOT NULL DEFAULT (UTC_TIMESTAMP(3)),
  PRIMARY KEY (`id`),
  UNIQUE KEY `Booking_bookingRef_key` (`bookingRef`),
  KEY `Booking_room_dates_idx` (`roomId`, `status`, `checkIn`, `checkOut`),
  CONSTRAINT `Booking_roomId_fkey` FOREIGN KEY (`roomId`) REFERENCES `Room` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `Booking_dates_chk` CHECK (`checkOut` > `checkIn`),
  CONSTRAINT `Booking_counts_chk` CHECK (`guests` >= 1 AND `nights` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
