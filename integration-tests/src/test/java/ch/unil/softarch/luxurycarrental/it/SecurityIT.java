package ch.unil.softarch.luxurycarrental.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Who may do what: the API refuses everything that is not explicitly allowed. */
class SecurityIT extends ApiTest {

    private static TestCustomer alice;
    private static TestCustomer bob;
    private static String bobsBooking;

    @BeforeAll
    static void twoCustomers() {
        alice = register();
        bob = register();
        var car = newCar("300.00", "1000.00");
        LocalDate start = LocalDate.now().plusDays(30);
        Http.Response booking = book(bob, car, start, start.plusDays(2));
        assertEquals(200, booking.status(), booking::toString);
        bobsBooking = booking.json().getString("bookingId");
    }

    @Test
    void withoutATokenOnlyPublicEndpointsAnswer() {
        assertEquals(200, Http.get("/cars/search", null).status());
        assertEquals(401, Http.get("/cars", null).status());          // the full list with maintenance cars is for staff
        assertEquals(401, Http.get("/bookings", null).status());
        assertEquals(401, Http.get("/customers/" + alice.id(), null).status());
    }

    @Test
    void aForgedOrTamperedTokenIsRejected() {
        assertEquals(401, Http.get("/customers/" + alice.id(), "abc.def").status());
        String tampered = alice.token().substring(0, alice.token().length() - 2) + "xx";
        assertEquals(401, Http.get("/customers/" + alice.id(), tampered).status());
    }

    @Test
    void aCustomerCannotSeeOrChangeAnotherCustomer() {
        assertEquals(403, Http.get("/customers/" + bob.id(), alice.token()).status());
        assertEquals(403, Http.get("/bookings/customer/" + bob.id(), alice.token()).status());
        assertEquals(403, Http.post("/customers/" + bob.id() + "/top-up",
                jakarta.json.Json.createObjectBuilder().add("amount", 1000).build(), alice.token()).status());
        assertEquals(403, Http.post("/bookings/" + bobsBooking + "/cancel", null, alice.token()).status());
    }

    @Test
    void aCustomerCannotUseAdministratorFunctions() {
        assertEquals(403, Http.get("/bookings", alice.token()).status());
        assertEquals(403, Http.get("/customers", alice.token()).status());
        assertEquals(403, Http.put("/customers/" + alice.id() + "/verified?value=true", null, alice.token()).status());
        assertEquals(403, Http.put("/customers/" + alice.id() + "/email-verified?value=true", null, alice.token()).status());
    }

    @Test
    void passwordHashesNeverLeaveTheServer() {
        String asAdmin = Http.get("/customers", adminToken()).body();
        assertFalse(asAdmin.contains("pbkdf2"), "a password hash was sent to the client");
        assertFalse(Http.get("/admins", adminToken()).body().contains("pbkdf2"));
    }

    @Test
    void aWrongPasswordGivesTheSameAnswerAsAnUnknownEmail() {
        var wrongPassword = Http.post("/customers/login", jakarta.json.Json.createObjectBuilder()
                .add("email", alice.email()).add("password", "nope").build(), null);
        var unknownEmail = Http.post("/customers/login", jakarta.json.Json.createObjectBuilder()
                .add("email", "nobody@example.com").add("password", "nope").build(), null);
        assertEquals(401, wrongPassword.status());
        assertEquals(wrongPassword.body(), unknownEmail.body());
    }
}
