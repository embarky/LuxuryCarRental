"""
Demo data seeder for the Luxury Car Rental API.

Runs automatically as the "seeder" service of docker-compose.yml when the database is
empty. Everything goes through the public REST API, so every business rule applies:

  1. log in as the default administrator (created by the server at start-up);
  2. create car types (13 real luxury models) and cars with matching images;
  3. create a few more administrators;
  4. register customers through the public sign-up, then verify most of them and top up
     their balance (a fixed demo customer is always created);
  5. create bookings and move some of them through the workflow: paid, picked up and
     returned, cancelled, rejected, or left waiting for payment.

Run by hand against a running API:
    pip install -r requirements.txt
    API_BASE_URL=http://localhost:8080/luxurycarrental/api python database_seeder.py
"""

import os
import random
import string
import sys
import time
import unicodedata
from datetime import date, timedelta
from typing import Any, Dict, List, Optional

import requests
from faker import Faker

# ------------------------------------------------------------------
# Configuration
# ------------------------------------------------------------------

BASE_URL = os.environ.get("API_BASE_URL", "http://127.0.0.1:8080/luxurycarrental/api").rstrip("/")
ADMIN_EMAIL = os.environ.get("DEFAULT_ADMIN_EMAIL", "admin@example.com")
ADMIN_PASSWORD = os.environ.get("DEFAULT_ADMIN_PASSWORD", "admin123")

SEED_ADMINS = int(os.environ.get("SEED_ADMINS", "3"))
SEED_CUSTOMERS = int(os.environ.get("SEED_CUSTOMERS", "60"))
SEED_CARS = int(os.environ.get("SEED_CARS", "40"))
SEED_BOOKINGS = int(os.environ.get("SEED_BOOKINGS", "120"))

# All generated customers share this password, so any of them can be used to log in.
CUSTOMER_PASSWORD = "customer123"

DEMO_CUSTOMER = {
    "firstName": "Demo",
    "lastName": "Customer",
    "email": "customer@example.com",
    "password": CUSTOMER_PASSWORD,
    "phoneNumber": "+41 21 000 00 00",
    "dateOfBirth": "1990-05-17",
    "drivingLicenseNumber": "CH-DEMO-0001",
    "drivingLicenseExpiryDate": "2034-12-31",
    "billingAddress": "Rue de la Gare 1, 1003 Lausanne",
}

fake = Faker(["fr_CH", "de_CH"])
random.seed(42)
Faker.seed(42)

# (brand, model, category, imagin make, imagin model family, engine, hp, top speed, 0-100, weight,
#  drive, gearbox, seats, base price per day in CHF, description, features)
CATALOGUE = [
    ("Porsche", "911 Carrera S", "Sport", "porsche", "911", "3.0L twin-turbo flat-6", 450, 308, 3.5, 1515,
     "REAR_WHEEL_DRIVE", "AUTOMATIC", 4, 690, "The rear-engined icon: precise, fast and usable every day.",
     ["Sport Chrono", "Bose sound", "Adaptive cruise"]),
    ("Porsche", "Cayenne", "SUV", "porsche", "cayenne", "3.0L turbo V6", 353, 248, 6.0, 2055,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 5, 420, "A sports car in SUV form, at home on the motorway and in the Alps.",
     ["Panoramic roof", "Heated seats", "Air suspension"]),
    ("Porsche", "Taycan 4S", "Electric", "porsche", "taycan", "Dual-motor electric", 530, 250, 4.0, 2140,
     "ELECTRIC_AWD", "ELECTRIC", 4, 560, "Porsche handling with an 800-volt electric drive.",
     ["Fast charging", "Matrix LED", "Head-up display"]),
    ("Mercedes-Benz", "G 63 AMG", "SUV", "mercedes", "gclass", "4.0L V8 biturbo", 585, 220, 4.5, 2560,
     "FOUR_WHEEL_DRIVE", "AUTOMATIC", 5, 780, "The legendary off-roader with a hand-built AMG V8.",
     ["Three differential locks", "Burmester sound", "Massage seats"]),
    ("Mercedes-Benz", "S 500", "Sedan", "mercedes", "sclass", "3.0L six-cylinder mild hybrid", 435, 250, 4.9, 2070,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 5, 520, "The reference in comfort for long journeys.",
     ["Rear-seat comfort pack", "Night vision", "Ambient lighting"]),
    ("Audi", "R8 V10", "Sport", "audi", "r8", "5.2L naturally aspirated V10", 570, 324, 3.4, 1590,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 2, 820, "A mid-engined V10 supercar that is easy to drive.",
     ["Carbon interior", "Bang & Olufsen sound", "Launch control"]),
    ("Audi", "RS 6 Avant", "Estate", "audi", "rs6", "4.0L V8 biturbo", 600, 280, 3.6, 2075,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 5, 540, "Supercar pace with room for the whole family and their luggage.",
     ["Matrix LED", "Sport exhaust", "Tow bar"]),
    ("Lamborghini", "Urus", "SUV", "lamborghini", "urus", "4.0L V8 biturbo", 650, 305, 3.6, 2200,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 5, 1150, "The fastest way to bring four friends to the mountains.",
     ["Anima drive modes", "Carbon brakes", "Panoramic roof"]),
    ("Bentley", "Continental GT", "Grand Tourer", "bentley", "continental", "4.0L V8", 550, 318, 4.0, 2165,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 4, 950, "Hand-finished grand tourer for crossing Europe in style.",
     ["Rotating display", "Naim audio", "Diamond-quilted seats"]),
    ("Land Rover", "Range Rover", "SUV", "land-rover", "range-rover", "4.4L V8 twin-turbo", 530, 250, 4.6, 2585,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 5, 640, "Quiet luxury on every surface, from Geneva to Zermatt.",
     ["Terrain response", "Meridian sound", "Executive seats"]),
    ("Tesla", "Model S Plaid", "Electric", "tesla", "models", "Tri-motor electric", 1020, 322, 2.1, 2190,
     "ELECTRIC_AWD", "ELECTRIC", 5, 380, "Over 1000 hp and a range for a weekend away.",
     ["Autopilot", "Premium audio", "Supercharger access"]),
    ("BMW", "M4 Competition", "Sport", "bmw", "m4", "3.0L twin-turbo inline-6", 530, 290, 3.5, 1725,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 4, 450, "A track-ready coupé that stays comfortable on the road.",
     ["Carbon bucket seats", "Laser lights", "Harman Kardon"]),
    ("BMW", "X5 M60i", "SUV", "bmw", "x5", "4.4L V8", 530, 250, 4.3, 2340,
     "ALL_WHEEL_DRIVE", "AUTOMATIC", 5, 390, "Versatile and powerful family SUV.",
     ["Sky lounge roof", "Driving assistant pro", "Third-zone climate"]),
]

COLOURS = ["Black", "White", "Silver", "Grey", "Blue", "Red"]
REJECTION_REASONS = [
    "Driving licence could not be verified",
    "Car needed for unplanned maintenance",
    "Customer asked us to cancel by phone",
]


# ------------------------------------------------------------------
# HTTP helpers
# ------------------------------------------------------------------

class Api:
    """Small client that keeps the administrator token and reports API errors."""

    def __init__(self, base_url: str):
        self.base = base_url
        self.http = requests.Session()
        self.http.headers.update({"Content-Type": "application/json", "Accept": "application/json"})

    def login_admin(self, email: str, password: str) -> None:
        data = self.call("POST", "/admins/login", {"email": email, "password": password})
        if not data:
            sys.exit("Cannot log in as the default administrator - check DEFAULT_ADMIN_EMAIL / DEFAULT_ADMIN_PASSWORD")
        self.http.headers["Authorization"] = "Bearer " + data["token"]

    def call(self, method: str, path: str, body: Optional[Dict[str, Any]] = None, quiet: bool = False):
        try:
            resp = self.http.request(method, self.base + path, json=body, timeout=30)
        except requests.RequestException as e:
            print(f"[{method} {path}] {e}")
            return None
        if resp.status_code >= 400:
            if not quiet:
                try:
                    message = resp.json().get("message")
                except ValueError:
                    message = resp.text[:200]
                print(f"[{method} {path}] {resp.status_code}: {message}")
            return None
        return resp.json() if resp.content else {}


def wait_for_api(timeout_s: int = 900) -> None:
    """Block until the API answers (Payara needs about a minute to deploy)."""
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        try:
            if requests.get(f"{BASE_URL}/cartypes", timeout=5).status_code == 200:
                print(f"API is up at {BASE_URL}")
                return
        except requests.RequestException:
            pass
        print("Waiting for the API...")
        time.sleep(5)
    sys.exit(f"API not reachable at {BASE_URL} after {timeout_s} s")


def ascii_slug(text: str) -> str:
    """Lower-case ASCII version of a name, for e-mail addresses (Zoë Müller -> zoe.muller)."""
    plain = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    return "".join(ch for ch in plain.lower() if ch.isalnum() or ch == ".")


def password() -> str:
    """A random password that satisfies the policy (letters and digits, 10 characters)."""
    letters = random.choices(string.ascii_letters, k=7)
    digits = random.choices(string.digits, k=3)
    chars = letters + digits
    random.shuffle(chars)
    return "".join(chars)


def image_url(make: str, family: str, colour: str) -> str:
    """Studio render of the model in the given colour (imagin.studio, no sign-up needed)."""
    return (f"https://cdn.imagin.studio/getimage?customer=img&make={make}"
            f"&modelFamily={family}&paintId={colour.lower()}&zoomType=fullscreen")


# ------------------------------------------------------------------
# Fleet
# ------------------------------------------------------------------

def seed_fleet(api: Api) -> List[Dict[str, Any]]:
    types = []
    for entry in CATALOGUE:
        (brand, model, category, _make, _family, engine, power, speed, accel, weight,
         drive, gearbox, seats, _price, description, features) = entry
        created = api.call("POST", "/cartypes", {
            "brand": brand, "model": model, "category": category, "engine": engine,
            "power": power, "maxSpeed": speed, "acceleration": accel, "weight": weight,
            "driveType": drive, "transmission": gearbox, "seats": seats,
            "description": description, "features": features,
        })
        if created:
            types.append((created, entry))
    print(f"Created {len(types)} car types")

    cars = []
    plates = set()
    for i in range(SEED_CARS):
        # every model at least once, then random ones
        created_type, entry = types[i] if i < len(types) else random.choice(types)
        make, family, base_price = entry[3], entry[4], entry[13]
        colour = random.choice(COLOURS)
        plate = None
        while plate is None or plate in plates:
            plate = f"{random.choice(['VD', 'GE', 'VS', 'ZH', 'BE'])} {random.randint(100000, 999999)}"
        plates.add(plate)
        car = api.call("POST", "/cars", {
            "carType": {"id": created_type["id"]},
            "licensePlate": plate,
            "color": colour,
            "dailyRentalPrice": base_price + random.choice([-40, -20, 0, 0, 20, 40]),
            "depositAmount": max(1000, round(base_price * 4, -2)),
            "imageUrl": image_url(make, family, colour),
            "status": "MAINTENANCE" if random.random() < 0.08 else "AVAILABLE",
            "registrationDate": str(date.today() - timedelta(days=random.randint(120, 1400))),
            "lastMaintenanceDate": str(date.today() - timedelta(days=random.randint(5, 150))),
            "insuranceExpiryDate": str(date.today() + timedelta(days=random.randint(90, 700))),
            "vin": "".join(random.choices(string.ascii_uppercase + string.digits, k=17)),
        })
        if car:
            cars.append(car)
    print(f"Created {len(cars)} cars")
    return cars


# ------------------------------------------------------------------
# People
# ------------------------------------------------------------------

def seed_admins(api: Api) -> None:
    for i in range(SEED_ADMINS):
        first, last = fake.first_name(), fake.last_name()
        username = ascii_slug(f"{first[0]}{last}").replace(".", "")[:20] + str(i)
        api.call("POST", "/admins", {
            "username": username, "name": f"{first} {last}",
            "email": f"{username}@luxurycarrental.local", "password": password(),
        })
    print(f"Created {SEED_ADMINS} more administrators")


def register_customer(api: Api, data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    # registration is public: send it without the admin token
    resp = requests.post(f"{BASE_URL}/customers", json=data, timeout=30)
    if resp.status_code >= 400:
        print(f"[register {data['email']}] {resp.status_code}: {resp.json().get('message')}")
        return None
    return resp.json()


def seed_customers(api: Api) -> List[Dict[str, Any]]:
    customers = []
    demo = register_customer(api, DEMO_CUSTOMER)
    if demo:
        api.call("PUT", f"/customers/{demo['id']}/verified?value=true")
        demo = api.call("POST", f"/customers/{demo['id']}/top-up", {"amount": 50000})
        customers.append(demo)

    for i in range(SEED_CUSTOMERS):
        first, last = fake.first_name(), fake.last_name()
        email = ascii_slug(f"{first}.{last}{i}") + "@example.com"
        customer = register_customer(api, {
            "firstName": first,
            "lastName": last,
            "email": email,
            "password": CUSTOMER_PASSWORD,
            "phoneNumber": f"+41 7{random.choice('6789')} {random.randint(100, 999)} {random.randint(10, 99)} {i % 100:02d}",
            "dateOfBirth": str(fake.date_of_birth(minimum_age=21, maximum_age=72)),
            "drivingLicenseNumber": f"CH-{random.randint(1000000, 9999999)}-{i}",
            "drivingLicenseExpiryDate": str(date.today() + timedelta(days=random.randint(400, 3600))),
            "billingAddress": fake.address().replace("\n", ", "),
        })
        if not customer:
            continue
        if random.random() < 0.85:
            api.call("PUT", f"/customers/{customer['id']}/verified?value=true")
            customer = api.call("POST", f"/customers/{customer['id']}/top-up",
                                {"amount": random.choice([3000, 5000, 8000, 15000, 25000])}) or customer
            customer["verifiedIdentity"] = True
        customers.append(customer)
    print(f"Registered {len(customers)} customers "
          f"({sum(1 for c in customers if c.get('verifiedIdentity'))} verified)")
    return customers


# ------------------------------------------------------------------
# Bookings
# ------------------------------------------------------------------

def seed_bookings(api: Api, cars: List[Dict[str, Any]], customers: List[Dict[str, Any]]) -> None:
    bookable = [c for c in cars if c.get("status") == "AVAILABLE"]
    counts = {"created": 0, "paid": 0, "completed": 0, "active": 0, "cancelled": 0, "rejected": 0, "taken": 0}
    today = date.today()

    for i in range(SEED_BOOKINGS):
        car = random.choice(bookable)
        customer = random.choice(customers)
        # about one in eight starts today, so it can be handed over (and returned) right away
        start = today if i % 8 == 0 else today + timedelta(days=random.randint(1, 75))
        end = start + timedelta(days=random.randint(1, 7))
        booking = api.call("POST", "/bookings", {
            "customerId": customer["id"], "carId": car["id"],
            "startDate": str(start), "endDate": str(end),
        }, quiet=True)
        if not booking:
            counts["taken"] += 1   # dates already booked: the overlap rule works
            continue
        counts["created"] += 1
        bid = booking["bookingId"]

        roll = random.random()
        if not customer.get("verifiedIdentity") or roll < 0.25:
            if roll < 0.08:
                api.call("POST", f"/bookings/{bid}/cancel")
                counts["cancelled"] += 1
            elif roll < 0.13:
                api.call("POST", f"/bookings/{bid}/reject", {"reason": random.choice(REJECTION_REASONS)})
                counts["rejected"] += 1
            continue                                   # stays waiting for payment

        if api.call("POST", f"/bookings/{bid}/pay", quiet=True) is None:
            continue                                   # e.g. balance too low: stays pending
        counts["paid"] += 1

        if start == today:
            pick = random.random()
            if pick < 0.4:
                continue                               # paid, hand-over due today: shows on the dashboard
            api.call("POST", f"/bookings/{bid}/pick-up")
            if pick < 0.75:
                api.call("POST", f"/bookings/{bid}/complete")
                counts["completed"] += 1
            else:
                counts["active"] += 1
        elif roll > 0.92:
            api.call("POST", f"/bookings/{bid}/reject", {"reason": random.choice(REJECTION_REASONS)})
            counts["rejected"] += 1

    print("Bookings: " + ", ".join(f"{k} {v}" for k, v in counts.items()))


# ------------------------------------------------------------------
# Main
# ------------------------------------------------------------------

def main() -> None:
    wait_for_api()

    if requests.get(f"{BASE_URL}/cartypes", timeout=10).json():
        print("Database already contains data - skipping seeding.")
        return

    api = Api(BASE_URL)
    api.login_admin(ADMIN_EMAIL, ADMIN_PASSWORD)

    print("\n▶ Fleet...")
    cars = seed_fleet(api)
    print("\n▶ Administrators...")
    seed_admins(api)
    print("\n▶ Customers...")
    customers = seed_customers(api)
    print("\n▶ Bookings...")
    seed_bookings(api, cars, customers)

    print("\n✅ Seeding complete.")
    print(f"Demo logins: {ADMIN_EMAIL} / {ADMIN_PASSWORD} (admin), "
          f"{DEMO_CUSTOMER['email']} / {CUSTOMER_PASSWORD} (customer)")
    print(f"Every generated customer also uses the password {CUSTOMER_PASSWORD}.")


if __name__ == "__main__":
    main()
