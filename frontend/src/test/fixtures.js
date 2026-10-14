// The three rooms the database seeds (database/init/02-seed.sql), shaped as the API returns them.
export const ROOMS = [
  {
    id: 101,
    name: "Deluxe Room",
    description: "A bright and comfortable retreat.",
    price: 4000,
    capacity: 2,
    facilities: ["AC", "WiFi", "TV"],
    features: ["Queen-size bed", "City-view window"],
    image: "/images/deluxe.jpg",
    available: true,
  },
  {
    id: 102,
    name: "Luxury Room",
    description: "A warmer, richer stay.",
    price: 7000,
    capacity: 3,
    facilities: ["AC", "WiFi", "Breakfast", "Room Service"],
    features: ["King-size bed", "Marble bathroom"],
    image: "/images/luxury.jpg",
    available: true,
  },
  {
    id: 103,
    name: "Suite Room",
    description: "The crown of StayEase.",
    price: 10000,
    capacity: 4,
    facilities: ["Balcony", "Premium View", "Mini Bar", "Room Service"],
    features: ["Private balcony", "Separate living area"],
    image: "/images/suite.jpg",
    available: true,
  },
];

export function jsonResponse(body, status = 200) {
  return Promise.resolve({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  });
}
