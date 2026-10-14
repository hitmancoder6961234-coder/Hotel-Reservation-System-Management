-- ============================================================================
--  StayEase - seed data: the 3 room types of the original application
--  (copied from the original scripts/seed.ts). Idempotent upsert.
-- ============================================================================

SET NAMES utf8mb4;

INSERT INTO `Room` (`id`, `name`, `description`, `price`, `capacity`, `facilities`, `features`, `image`, `available`)
VALUES
(101, 'Deluxe Room',
 'A bright and comfortable retreat designed for modern travellers. The Deluxe Room features a plush queen-size bed, a curated work desk and floor-to-ceiling windows that fill the space with natural light. Perfect for short city stays, couples on a weekend escape, or solo guests who appreciate calm, uncluttered comfort.',
 4000, 2, 'AC, WiFi, TV',
 'Queen-size bed, City-view window, Work desk, Rain shower, Daily housekeeping, Electronic safe',
 '/images/deluxe.jpg', 1),
(102, 'Luxury Room',
 'Step into a warmer, richer stay. The Luxury Room pairs a king-size bed with soft ambient lighting, a marble bathroom and a relaxing lounge chair by the window. Wake up to a complimentary breakfast and enjoy attentive room service throughout your visit — an ideal choice for families of three or guests who like extra space to unwind.',
 7000, 3, 'AC, WiFi, Breakfast, Room Service',
 'King-size bed, Marble bathroom, Lounge chair, Complimentary breakfast, 24x7 room service, Mini fridge',
 '/images/luxury.jpg', 1),
(103, 'Suite Room',
 'The crown of StayEase. Our Suite Room opens onto a private balcony with a premium panoramic view, with a separate living area, a well-stocked mini bar and a lavish king-size bedroom. Whether it is a honeymoon, an anniversary or a well-earned treat, the suite turns any night into an occasion for up to four guests.',
 10000, 4, 'Balcony, Premium View, Mini Bar, Room Service',
 'Private balcony, Separate living area, Panoramic view, Stocked mini bar, Bathtub + walk-in shower, Priority check-in',
 '/images/suite.jpg', 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`), `description` = VALUES(`description`), `price` = VALUES(`price`),
  `capacity` = VALUES(`capacity`), `facilities` = VALUES(`facilities`), `features` = VALUES(`features`),
  `image` = VALUES(`image`), `available` = VALUES(`available`);
