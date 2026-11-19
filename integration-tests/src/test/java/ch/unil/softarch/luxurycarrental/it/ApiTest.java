package ch.unil.softarch.luxurycarrental.it;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class of the integration tests: starts the environment and creates test data through the
 * public API, exactly as a client would. Every test creates its own customers and cars, so the
 * tests do not depend on each other or on their order.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class ApiTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");

    /** A customer created for a test, with their login token. */
    record TestCustomer(String id, String email, String token) {
    }

    @BeforeAll
    static void startEnvironment() {
        TestEnvironment.start();
    }

    static String adminToken() {
        Http.Response r = Http.post("/admins/login", Json.createObjectBuilder()
                .add("email", TestEnvironment.ADMIN_EMAIL)
                .add("password", TestEnvironment.ADMIN_PASSWORD).build(), null);
        assertEquals(200, r.status(), () -> "admin login: " + r);
        return r.json().getString("token");
    }

    /** A new car (with its own car type) that nobody has booked yet. */
    static JsonObject newCar(String dailyPrice, String deposit) {
        String admin = adminToken();
        int n = COUNTER.incrementAndGet();
        Http.Response type = Http.post("/cartypes", Json.createObjectBuilder()
                .add("brand", "Test").add("model", "Model " + n).add("category", "Sport")
                .add("engine", "V8").add("power", 500).add("maxSpeed", 300).add("seats", 2)
                .add("transmission", "AUTOMATIC").add("driveType", "REAR_WHEEL_DRIVE").build(), admin);
        assertEquals(200, type.status(), () -> "create car type: " + type);
        Http.Response car = Http.post("/cars", Json.createObjectBuilder()
                .add("licensePlate", "IT " + System.nanoTime() % 1_000_000 + "-" + n)
                .add("carType", Json.createObjectBuilder().add("id", type.json().getString("id")))
                .add("dailyRentalPrice", Json.createValue(new java.math.BigDecimal(dailyPrice)))
                .add("depositAmount", Json.createValue(new java.math.BigDecimal(deposit)))
                .add("color", "Black").build(), admin);
        assertEquals(200, car.status(), () -> "create car: " + car);
        return car.json();
    }

    /** Registers a customer through the public sign-up and logs them in. */
    static TestCustomer register() {
        int n = COUNTER.incrementAndGet();
        String email = "it.customer" + n + "." + System.nanoTime() % 100_000 + "@example.com";
        Http.Response r = Http.post("/customers", Json.createObjectBuilder()
                .add("firstName", "Test").add("lastName", "Customer " + n)
                .add("email", email).add("password", "secret2026")
                .add("phoneNumber", "+41 79 " + (100 + n) + " " + String.format("%02d %02d", n % 100, (n * 7) % 100))
                .add("dateOfBirth", "1995-03-14")
                .add("drivingLicenseNumber", "IT-" + System.nanoTime())
                .add("drivingLicenseExpiryDate", LocalDate.now().plusYears(5).toString()).build(), null);
        assertEquals(200, r.status(), () -> "register: " + r);
        String id = r.json().getString("id");
        return new TestCustomer(id, email, login(email, "secret2026"));
    }

    /** A customer who can pay: email confirmed with the code from the welcome email, licence verified, money on the account. */
    static TestCustomer readyCustomer(String balance) {
        TestCustomer c = register();
        Http.Response confirmed = Http.put("/customers/" + c.id() + "/email-confirmation",
                Json.createObjectBuilder().add("code", codeFromLastEmail(c.email())).build(), c.token());
        assertEquals(200, confirmed.status(), () -> "confirm email: " + confirmed);
        String admin = adminToken();
        assertEquals(200, Http.put("/customers/" + c.id() + "/verified?value=true", null, admin).status());
        Http.Response topUp = Http.post("/customers/" + c.id() + "/top-up",
                Json.createObjectBuilder().add("amount", Json.createValue(new java.math.BigDecimal(balance))).build(), c.token());
        assertEquals(200, topUp.status(), () -> "top up: " + topUp);
        return c;
    }

    static String login(String email, String password) {
        Http.Response r = Http.post("/customers/login",
                Json.createObjectBuilder().add("email", email).add("password", password).build(), null);
        assertEquals(200, r.status(), () -> "customer login: " + r);
        return r.json().getString("token");
    }

    static Http.Response book(TestCustomer c, JsonObject car, LocalDate start, LocalDate end) {
        return Http.post("/bookings", Json.createObjectBuilder()
                .add("carId", car.getString("id"))
                .add("startDate", start.toString())
                .add("endDate", end.toString()).build(), c.token());
    }

    static java.math.BigDecimal balanceOf(TestCustomer c) {
        return Http.get("/customers/" + c.id(), c.token()).json().getJsonNumber("balance").bigDecimalValue();
    }

    /**
     * The 6-digit code in the newest email to this address. Emails are sent in the background
     * after the transaction commits, so this waits a little for it to arrive.
     */
    static String codeFromLastEmail(String to) {
        String search = TestEnvironment.mailpitUrl() + "/api/v1/search?limit=1&query="
                + URLEncoder.encode("to:\"" + to + "\"", StandardCharsets.UTF_8);
        for (int attempt = 0; attempt < 40; attempt++) {
            Http.Response found = Http.get(search, null);
            if (found.status() == 200 && !found.json().getJsonArray("messages").isEmpty()) {
                String id = found.json().getJsonArray("messages").getJsonObject(0).getString("ID");
                String text = Http.get(TestEnvironment.mailpitUrl() + "/api/v1/message/" + id, null)
                        .json().getString("Text", "");
                Matcher m = SIX_DIGITS.matcher(text);
                if (m.find()) {
                    return m.group(1);
                }
            }
            sleep(250);
        }
        return fail("No email with a code arrived for " + to);
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
