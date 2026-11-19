"""
Full data seeder for LuxuryCarRental API.

Runs automatically as the "seeder" service of docker-compose.yml on an empty
database. It can also be run by hand against a running API:
    pip install -r requirements.txt
    API_BASE_URL=http://localhost:8080/luxurycarrental/api python database_seeder.py

Features:
- Inserts Admins, Customers, CarTypes, Cars (with brand+color matched images via imagin.studio),
  and Bookings.
- Uses Faker to create realistic personal data.
- Detailed English comments for every function and important steps.
- Safe handling of HTTP responses and basic retry/backoff when reading lists.
- Keeps small sleep between requests to avoid spamming the local server.

Note:
- This script assumes your API supports the endpoints:
  /admins, /customers, /cartypes, /cars, /bookings
  and that POST body shapes in this script match your API contract.
- Image URLs are generated using imagin.studio rendering parameters:
  https://cdn.imagin.studio/getimage?customer=img&make=<make>&modelFamily=<model>&paintId=<paint>
  This is a parameterized rendering endpoint (no signup required for basic usage).
"""

import os
import sys
import requests
from faker import Faker
import random
import string
import time
from typing import List, Dict, Any, Optional

# ------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------

# Base URL of the API. In Docker Compose this is set to the app container.
BASE_URL = os.environ.get("API_BASE_URL", "http://127.0.0.1:8080/luxurycarrental/api").rstrip("/")

# How many records to create (override with environment variables)
SEED_ADMINS = int(os.environ.get("SEED_ADMINS", "10"))
SEED_CUSTOMERS = int(os.environ.get("SEED_CUSTOMERS", "200"))
SEED_CARTYPES = int(os.environ.get("SEED_CARTYPES", "40"))
SEED_CARS = int(os.environ.get("SEED_CARS", "200"))
SEED_BOOKINGS = int(os.environ.get("SEED_BOOKINGS", "400"))

# Fixed accounts so that visitors can log in to the demo
DEMO_ADMIN = {
    "username": "admin",
    "password": "admin123",
    "name": "Demo Admin",
    "email": "admin@example.com",
}
DEMO_CUSTOMER = {
    "firstName": "Demo",
    "lastName": "Customer",
    "password": "customer123",
    "email": "customer@example.com",
    "phoneNumber": "+41 21 000 00 00",
    "drivingLicenseNumber": "DL000000",
    "drivingLicenseExpiryDate": "2030-12-31T23:59:59Z",
    "age": 30,
    "verifiedIdentity": True,
    "billingAddress": "Rue de la Gare 1, 1003 Lausanne, Switzerland",
    "balance": 50000,
}

# Default HTTP headers used for JSON requests
HEADERS = {
    "Content-Type": "application/json",
    "Accept": "application/json"
}

# Faker instance for generating realistic fake data (US locale)
fake = Faker("en_US")

# Small pause between requests to avoid overloading the API
REQUEST_DELAY = 0.01

# ------------------------------------------------------------------
# Utility functions
# ------------------------------------------------------------------

def realistic_username(first_name: str, last_name: str, index: int) -> str:
    """
    Generate a realistic username from first name, last name and an index.
    The function uses a few pattern templates and chooses randomly to create diversity.
    """
    patterns = [
        f"{first_name.lower()}.{last_name.lower()}{index}",
        f"{first_name.lower()}_{last_name.lower()}{index}",
        f"{first_name[0].lower()}{last_name.lower()}{random.randint(1,99)}",
        f"{first_name.lower()}{random.randint(10,99)}"
    ]
    return random.choice(patterns)


def random_password(length: int = 8) -> str:
    """
    Create a random alphanumeric password of given length.
    Uses both upper/lowercase letters and digits for realism.
    """
    chars = string.ascii_letters + string.digits
    return ''.join(random.choices(chars, k=length))


def safe_get_json(url: str, max_retries: int = 3, backoff: float = 0.1) -> Optional[List[Dict[str, Any]]]:
    """
    Perform a GET request and return parsed JSON if successful.
    Retries a few times with exponential backoff on transient errors.
    Returns None if unable to retrieve JSON.
    """
    attempt = 0
    while attempt < max_retries:
        try:
            resp = requests.get(url, headers=HEADERS, timeout=10)
            if resp.status_code == 200:
                # Expecting list-like JSON for resources
                return resp.json()
            else:
                # Non-200: break early and return None (the caller will handle)
                print(f"[GET] {url} returned {resp.status_code}: {resp.text}")
                return None
        except requests.RequestException as e:
            attempt += 1
            print(f"[GET] Exception on {url} attempt {attempt}: {e}. Retrying in {backoff} s...")
            time.sleep(backoff)
            backoff *= 2
    print(f"[GET] Failed to fetch {url} after {max_retries} attempts.")
    return None


# ------------------------------------------------------------------
# Image URL generation (imagin.studio)
# ------------------------------------------------------------------

def generate_real_car_image(brand: str, color: str) -> str:
    """
    Generate a production-ready image URL for a car using imagin.studio rendering endpoint.

    - brand: user-friendly brand name (e.g., 'BMW', 'Audi', 'Tesla')
    - color: one of the supported color names we use (e.g., 'Red', 'Black', 'Silver')

    The function maps user-visible brand/color to imagin's query parameters.
    The returned URL points to a rendered image that visually matches brand + paint.
    """
    # Map the visible brand to imagin parameters (make and modelFamily).
    # These modelFamily names are approximate; change as needed to match your preferred images.
    brand_map = {
        "BMW": ("bmw", "3"),
        "Audi": ("audi", "a4"),
        "Mercedes": ("mercedes", "cclass"),
        "Toyota": ("toyota", "corolla"),
        "Honda": ("honda", "civic"),
        "Ford": ("ford", "focus"),
        "Tesla": ("tesla", "model3")
    }

    # Map simple color names to paint tokens used in the URL.
    color_map = {
        "Red": "red",
        "Blue": "blue",
        "Black": "black",
        "White": "white",
        "Silver": "silver"
    }

    make, model_family = brand_map.get(brand, ("tesla", "model3"))
    paint = color_map.get(color, "black")

    # Build the imagin.studio URL.
    # We include customer=img for the default consumer output.
    # modelFamily selects a recognizable model family; paintId selects color.
    url = (
        f"https://cdn.imagin.studio/getimage?"
        f"customer=img&make={make}&modelFamily={model_family}&paintId={paint}"
    )

    # Return the fully constructed URL. The frontend can use this directly in <img src="...">.
    return url


# ------------------------------------------------------------------
# Admin insertion
# ------------------------------------------------------------------

def insert_admins(base_url: str, count: int = 100) -> None:
    """
    Insert 'count' admin users to the API.
    Each admin contains: username, password, name, email.
    """
    url = f"{base_url}/admins"
    for i in range(count):
        first = fake.first_name()
        last = fake.last_name()
        username = realistic_username(first, last, i)
        password = random_password(8)
        email = f"{username}@example.com"

        payload = {
            "username": username,
            "password": password,
            "name": f"{first} {last}",
            "email": email
        }

        try:
            resp = requests.post(url, json=payload, headers=HEADERS, timeout=10)
            if resp.status_code in (200, 201):
                print(f"[Admin {i+1}] Inserted: {username} / {password}")
            else:
                print(f"[Admin {i+1}] Failed {resp.status_code}: {resp.text}")
        except requests.RequestException as e:
            print(f"[Admin {i+1}] Exception: {e}")

        time.sleep(REQUEST_DELAY)


# ------------------------------------------------------------------
# Customer insertion
# ------------------------------------------------------------------

def insert_customers(base_url: str, count: int = 100) -> None:
    """
    Insert 'count' customers to the API with realistic customer fields.
    Fields include phoneNumber, drivingLicenseNumber, age, billingAddress, balance, etc.
    """
    url = f"{base_url}/customers"
    for i in range(count):
        first = fake.first_name()
        last = fake.last_name()
        username = realistic_username(first, last, i)
        password = random_password(8)
        phone = fake.phone_number()
        driving_license = f"DL{random.randint(100000, 999999)}"
        age = random.randint(18, 70)
        address = fake.address().replace("\n", ", ")
        balance = round(random.uniform(0, 1000), 2)

        payload = {
            "firstName": first,
            "lastName": last,
            "password": password,
            "email": f"{username}@example.com",
            "phoneNumber": phone,
            "drivingLicenseNumber": driving_license,
            # ISO-like expiry date string for driving license
            "drivingLicenseExpiryDate": "2030-12-31T23:59:59Z",
            "age": age,
            "verifiedIdentity": False,
            "billingAddress": address,
            "balance": balance
        }

        try:
            resp = requests.post(url, json=payload, headers=HEADERS, timeout=10)
            if resp.status_code in (200, 201):
                print(f"[Customer {i+1}] Inserted: {username} / {password}")
            else:
                print(f"[Customer {i+1}] Failed {resp.status_code}: {resp.text}")
        except requests.RequestException as e:
            print(f"[Customer {i+1}] Exception: {e}")

        time.sleep(REQUEST_DELAY)


# ------------------------------------------------------------------
# CarType insertion
# ------------------------------------------------------------------

def insert_cartypes(base_url: str, count: int = 20) -> None:
    """
    Insert 'count' car types (templates for cars) into the API.
    A CarType usually describes brand, model, engine, seats, power, features, etc.
    These will be referenced by Car records via carType.id.
    """
    url = f"{base_url}/cartypes"

    # Candidate values for random generation
    brands = ["BMW", "Audi", "Mercedes", "Toyota", "Honda", "Ford", "Tesla"]
    categories = ["SUV", "SEDAN", "SPORTS", "TRUCK", "COUPE"]
    transmissions = ["AUTOMATIC", "MANUAL"]
    drive_types = ["FOUR_WHEEL_DRIVE", "FRONT_WHEEL_DRIVE", "REAR_WHEEL_DRIVE"]

    for i in range(count):
        brand = random.choice(brands)
        # Use a short random word as a model token (non-critical for generation)
        model = fake.word().capitalize()

        payload = {
            "brand": brand,
            "model": model,
            "description": f"{brand} {model} high performance model",
            "category": random.choice(categories),
            "transmission": random.choice(transmissions),
            "driveType": random.choice(drive_types),
            "engine": f"{random.randint(1,4)}.{random.randint(0,9)}L",
            "acceleration": round(random.uniform(4.0, 10.0), 1),  # 0-100km/h approx
            "maxSpeed": random.randint(160, 300),                 # km/h
            "power": random.randint(100, 500),                    # horsepower approx
            "seats": random.choice([2, 4, 5, 7]),
            "weight": round(random.uniform(1000, 2500), 1),
            "features": ["Navigation", "Bluetooth", "Leather Seats", "Parking Assist"]
        }

        try:
            resp = requests.post(url, json=payload, headers=HEADERS, timeout=10)
            if resp.status_code in (200, 201):
                # Many APIs return the created resource with an ID in the body; we don't require it here.
                print(f"[CarType {i+1}] Inserted {brand} {model}")
            else:
                print(f"[CarType {i+1}] Failed {resp.status_code}: {resp.text}")
        except requests.RequestException as e:
            print(f"[CarType {i+1}] Exception: {e}")

        time.sleep(REQUEST_DELAY)


def get_cartype_ids(base_url: str) -> List[str]:
    """
    Retrieve the list of CarType resource objects and return a list of IDs.
    If the API returns a list of JSON objects, we pick the 'id' field.
    """
    url = f"{base_url}/cartypes"
    data = safe_get_json(url)
    if not data:
        return []
    # Defensive: ensure items with 'id' exist
    return [str(item["id"]) for item in data if "id" in item]


# ------------------------------------------------------------------
# Car insertion (brand+color matched image)
# ------------------------------------------------------------------

def insert_cars(base_url: str, count: int = 50) -> None:
    """
    Insert 'count' cars into the API. Each Car references a CarType by id.
    The function:
      1. retrieves available CarType objects
      2. selects random carType for each car
      3. chooses a color and produces a brand+color matched image URL via imagin.studio
      4. posts the Car object to /cars
    """
    url = f"{base_url}/cars"

    # Get the available CarType IDs to link cars to a type.
    cartype_list = safe_get_json(f"{base_url}/cartypes")
    if not cartype_list:
        print("❌ No CarTypes available. Please insert CarTypes first.")
        return

    # Build a mapping from id -> cartype entry for convenient access to brand/metadata.
    cartypes_by_id = {str(ct["id"]): ct for ct in cartype_list if "id" in ct}

    cartype_ids = list(cartypes_by_id.keys())
    if not cartype_ids:
        print("❌ No valid CarType IDs present in response.")
        return

    # Supported color options (kept in sync with the generate_real_car_image mapping)
    colors = ["Red", "Blue", "Black", "White", "Silver"]

    for i in range(count):
        # Choose random CarType ID and read brand (fallback to 'Tesla' if brand missing)
        cartype_id = random.choice(cartype_ids)
        cartype = cartypes_by_id.get(cartype_id, {})
        brand = cartype.get("brand", "Tesla")  # default brand to Tesla if missing

        # Choose a realistic color for the car
        color = random.choice(colors)

        # Build an image URL that reflects the brand & color
        image_url = generate_real_car_image(brand, color)

        # Build the Car payload. Make sure field names match your API contract.
        payload = {
            "carType": {"id": cartype_id},
            "color": color,
            "dailyRentalPrice": round(random.uniform(60, 400), 2),
            "depositAmount": round(random.uniform(300, 1200), 2),
            "imageUrl": image_url,
            # Dates are expressed as ISO-ish strings where appropriate.
            "insuranceExpiryDate": "2026-12-01",
            "lastMaintenanceDate": "2025-10-10",
            "licensePlate": fake.license_plate(),
            "registrationDate": "2025-03-22",
            "status": "AVAILABLE",  # or match your enum values if different
            "vin": "VIN" + str(random.randint(100000, 999999))
        }

        try:
            resp = requests.post(url, json=payload, headers=HEADERS, timeout=10)
            if resp.status_code in (200, 201):
                print(f"[Car {i+1}] Inserted VIN={payload['vin']} | Brand={brand} | Color={color}")
            else:
                print(f"[Car {i+1}] Failed {resp.status_code}: {resp.text}")
        except requests.RequestException as e:
            print(f"[Car {i+1}] Exception: {e}")

        time.sleep(REQUEST_DELAY)


# ------------------------------------------------------------------
# Booking insertion
# ------------------------------------------------------------------

def get_car_ids(base_url: str) -> List[str]:
    """
    Get all car resource IDs from /cars endpoint.
    Returns empty list if fetch fails.
    """
    data = safe_get_json(f"{base_url}/cars")
    if not data:
        return []
    return [str(item["id"]) for item in data if "id" in item]


def get_customer_ids(base_url: str) -> List[str]:
    """
    Get all customer resource IDs from /customers endpoint.
    Returns empty list if fetch fails.
    """
    data = safe_get_json(f"{base_url}/customers")
    if not data:
        return []
    return [str(item["id"]) for item in data if "id" in item]


def insert_bookings(base_url: str, count: int = 50) -> None:
    """
    Insert 'booking_count' bookings linking a customer and a car.
    Logic ensures every available car and customer is used at least once
    if booking_count is sufficient.
    """
    url = f"{base_url}/bookings"

    car_data = safe_get_json(f"{base_url}/cars")
    customer_data = safe_get_json(f"{base_url}/customers")

    if not car_data or not customer_data:
        print("❌ No available Cars or Customers to create bookings. Ensure both resources exist.")
        return

    car_ids = [str(item["id"]) for item in car_data if "id" in item]
    customer_ids = [str(item["id"]) for item in customer_data if "id" in item]

    if not car_ids or not customer_ids:
        print("❌ Car or Customer lists are empty.")
        return

    # --- Strategy for full coverage ---
    
    # 1. Start with a list of IDs that MUST be covered (all cars, all customers).
    required_cars = list(car_ids)
    required_customers = list(customer_ids)

    # 2. Total IDs needed for guaranteed coverage
    initial_bookings_needed = len(required_cars) + len(required_customers)

    # 3. If there are fewer total bookings than needed for initial coverage, adjust the loop count.
    #    The loop should run for at least `max(booking_count, initial_bookings_needed)` times
    #    to guarantee the required bookings are made.
    total_bookings = max(count, initial_bookings_needed) 

    # --- Booking Loop ---
    for i in range(total_bookings):
        
        # Priority 1: Use a required Car or Customer if one is left.
        if required_cars:
            car_id = required_cars.pop(0)  # Use and remove the first required car
            customer_id = random.choice(customer_ids) # Pair it with any customer
        elif required_customers:
            customer_id = required_customers.pop(0) # Use and remove the first required customer
            car_id = random.choice(car_ids)         # Pair it with any car
        else:
            # Priority 2: All required items used, now choose randomly for the remaining bookings.
            car_id = random.choice(car_ids)
            customer_id = random.choice(customer_ids)

        # Use Faker to get a start date in the near past/future and an end date afterwards.
        # Ensure end date is always on or after start date.
        start_date = fake.date_between(start_date="-30d", end_date="today")
        end_date_str = str(fake.date_between_dates(date_start=start_date, date_end=fake.date_between(start_date="today", end_date="+30d")))

        payload = {
            "carId": car_id,
            "customerId": customer_id,
            "startDate": str(start_date),  # e.g., "2025-11-10"
            "endDate": end_date_str        # e.g., "2025-11-12"
        }

        try:
            resp = requests.post(url, json=payload, headers=HEADERS, timeout=10)
            if resp.status_code in (200, 201):
                # The total number of bookings may exceed the initial booking_count if
                # there were more unique cars/customers than the count requested.
                print(f"[Booking {i+1}] Inserted: car={payload['carId']} customer={payload['customerId']}")
            else:
                print(f"[Booking {i+1}] Failed {resp.status_code}: {resp.text}")
        except requests.RequestException as e:
            print(f"[Booking {i+1}] Exception: {e}")

        time.sleep(REQUEST_DELAY)

    # Print a summary of the booking strategy
    if total_bookings > count:
        print(f"\n💡 Note: Increased total bookings from requested {count} to {total_bookings} to ensure all Cars and Customers were included at least once.")
    
# You would replace the existing insert_bookings function with this revised one.


# ------------------------------------------------------------------
# Main runner
# ------------------------------------------------------------------

def wait_for_api(base_url: str, timeout_s: int = 900) -> None:
    """
    Block until the API answers, so the seeder can start together with the
    application server (Payara needs about a minute to deploy).
    """
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        try:
            if requests.get(f"{base_url}/cartypes", headers=HEADERS, timeout=5).status_code == 200:
                print(f"API is up at {base_url}")
                return
        except requests.RequestException:
            pass
        print("Waiting for the API...")
        time.sleep(5)
    sys.exit(f"API not reachable at {base_url} after {timeout_s} s")


def insert_demo_accounts(base_url: str) -> None:
    """Create one admin and one customer with known credentials."""
    for path, payload in (("admins", DEMO_ADMIN), ("customers", DEMO_CUSTOMER)):
        resp = requests.post(f"{base_url}/{path}", json=payload, headers=HEADERS, timeout=10)
        status = "created" if resp.status_code in (200, 201) else f"failed {resp.status_code}: {resp.text}"
        print(f"[Demo {path[:-1]}] {payload['email']} / {payload['password']} {status}")


def main():
    """
    Main orchestration function:
      0. Wait for the API; stop if the database already contains data
      1. Insert demo accounts with known credentials
      2. Insert Admins
      3. Insert Customers
      4. Insert CarTypes
      5. Insert Cars (brand+color matched images)
      6. Insert Bookings
    Counts are set with the SEED_* environment variables.
    """
    base_url = BASE_URL
    wait_for_api(base_url)

    # Seed only an empty database, so restarting the stack does not duplicate data
    if safe_get_json(f"{base_url}/cartypes"):
        print("Database already contains data - skipping seeding.")
        return

    print("\n▶ Inserting demo accounts...")
    insert_demo_accounts(base_url)

    # 1) Admins
    print("\n▶ Inserting Admins...")
    insert_admins(base_url, count=SEED_ADMINS)

    # 2) Customers
    print("\n▶ Inserting Customers...")
    insert_customers(base_url, count=SEED_CUSTOMERS)

    # 3) CarTypes
    print("\n▶ Inserting CarTypes...")
    insert_cartypes(base_url, count=SEED_CARTYPES)

    # 4) Cars (requires CarTypes to exist)
    print("\n▶ Inserting Cars...")
    insert_cars(base_url, count=SEED_CARS)

    # 5) Bookings (requires Cars and Customers)
    print("\n▶ Inserting Bookings...")
    insert_bookings(base_url, count=SEED_BOOKINGS)

    print("\n✅ Seeding complete.")
    print("Demo logins: admin@example.com / admin123, customer@example.com / customer123")


if __name__ == "__main__":
    main()