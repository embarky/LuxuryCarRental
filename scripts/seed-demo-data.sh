#!/usr/bin/env bash
# Fills an empty database with demo data through the public REST API.
# Usage: ./scripts/seed-demo-data.sh [API_BASE_URL]
set -euo pipefail

API="${1:-http://localhost:8080/luxurycarrental/api}"

post() {
  curl -fsS -X POST "$API/$1" -H "Content-Type: application/json" -d "$2"
}

# Extract the first "id" field from a JSON response
id_of() {
  sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([0-9a-fA-F-]\{36\}\)".*/\1/p'
}

echo "Waiting for $API ..."
for _ in $(seq 1 60); do
  curl -fsS "$API/cartypes" >/dev/null 2>&1 && break
  sleep 3
done

echo "Creating demo admin (admin@example.com / admin123)"
post admins '{"username":"admin","password":"admin123","name":"Demo Admin","email":"admin@example.com"}' >/dev/null

add_type_and_car() {
  local type_json="$1" plate="$2" vin="$3" color="$4" price="$5" deposit="$6"
  local type_id
  type_id=$(post cartypes "$type_json" | id_of)
  post cars "{\"licensePlate\":\"$plate\",\"vin\":\"$vin\",\"color\":\"$color\",
    \"dailyRentalPrice\":$price,\"depositAmount\":$deposit,\"status\":\"AVAILABLE\",
    \"registrationDate\":\"2024-03-01\",\"lastMaintenanceDate\":\"2025-10-01\",
    \"insuranceExpiryDate\":\"2027-03-01\",\"carType\":{\"id\":\"$type_id\"}}" >/dev/null
  echo "  + $plate"
}

echo "Creating car types and cars"
add_type_and_car '{"category":"Sport","brand":"Porsche","model":"911 Carrera S","engine":"3.0L twin-turbo flat-6",
  "power":450,"maxSpeed":308,"acceleration":3.5,"weight":1515,"driveType":"REAR_WHEEL_DRIVE",
  "transmission":"AUTOMATIC","seats":4,"description":"Iconic rear-engine sports car.",
  "features":["GPS","Bluetooth","Sport Chrono"]}' "VD-911001" "WP0ZZZ99ZRS000001" "Guards Red" 890 5000

add_type_and_car '{"category":"SUV","brand":"Range Rover","model":"Sport P530","engine":"4.4L twin-turbo V8",
  "power":530,"maxSpeed":250,"acceleration":4.5,"weight":2410,"driveType":"ALL_WHEEL_DRIVE",
  "transmission":"AUTOMATIC","seats":5,"description":"Luxury performance SUV.",
  "features":["GPS","Panoramic roof","Heated seats"]}' "VD-530002" "SALWA2AE0RA000002" "Carpathian Grey" 650 4000

add_type_and_car '{"category":"Electric","brand":"Tesla","model":"Model S Plaid","engine":"Tri-motor electric",
  "power":1020,"maxSpeed":322,"acceleration":2.1,"weight":2190,"driveType":"ELECTRIC_AWD",
  "transmission":"ELECTRIC","seats":5,"description":"High-performance electric sedan.",
  "features":["Autopilot","Bluetooth","Premium audio"]}' "VD-100003" "5YJSA1E60RF000003" "Pearl White" 520 3000

echo "Done. Open http://localhost:8080/LuxuryCarRental/"
