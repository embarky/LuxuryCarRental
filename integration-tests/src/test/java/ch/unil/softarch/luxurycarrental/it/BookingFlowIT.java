package ch.unil.softarch.luxurycarrental.it;

import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** A customer's way from sign-up to a paid and cancelled booking, with the money checked at each step. */
class BookingFlowIT extends ApiTest {

    @Test
    void signUpConfirmEmailBookPayAndCancel() {
        TestCustomer c = register();
        var car = newCar("400.00", "1500.00");
        LocalDate start = LocalDate.now().plusDays(20);

        // Reserving works right after sign-up; paying needs a confirmed email
        Http.Response booking = book(c, car, start, start.plusDays(3));
        assertEquals(200, booking.status(), booking::toString);
        String id = booking.json().getString("bookingId");
        assertEquals("PENDING", booking.json().getString("bookingStatus"));
        assertEquals(new BigDecimal("2700.00"), booking.json().getJsonNumber("amountDue").bigDecimalValue());

        Http.Response tooEarly = Http.post("/bookings/" + id + "/pay", null, c.token());
        assertEquals(400, tooEarly.status());
        assertTrue(tooEarly.message().contains("email"), tooEarly::toString);

        // The code from the welcome email confirms the address; a wrong code does not
        assertEquals(400, Http.put("/customers/" + c.id() + "/email-confirmation",
                Json.createObjectBuilder().add("code", "000000").build(), c.token()).status());
        Http.Response confirmed = Http.put("/customers/" + c.id() + "/email-confirmation",
                Json.createObjectBuilder().add("code", codeFromLastEmail(c.email())).build(), c.token());
        assertEquals(200, confirmed.status(), confirmed::toString);
        assertTrue(confirmed.json().getBoolean("emailVerified"));

        // Still not possible: the licence has not been checked yet
        assertEquals(400, Http.post("/bookings/" + id + "/pay", null, c.token()).status());
        assertEquals(200, Http.put("/customers/" + c.id() + "/verified?value=true", null, adminToken()).status());

        // Not enough money: refused, nothing changes
        Http.Response broke = Http.post("/bookings/" + id + "/pay", null, c.token());
        assertEquals(400, broke.status());
        assertTrue(broke.message().contains("Insufficient balance"), broke::toString);

        Http.post("/customers/" + c.id() + "/top-up",
                Json.createObjectBuilder().add("amount", 5000).build(), c.token());
        Http.Response paid = Http.post("/bookings/" + id + "/pay", null, c.token());
        assertEquals(200, paid.status(), paid::toString);
        assertEquals("CONFIRMED", paid.json().getString("bookingStatus"));
        assertEquals(new BigDecimal("2300.00"), balanceOf(c));

        // More than 48 hours before pick-up: cancelling refunds everything
        Http.Response cancelled = Http.post("/bookings/" + id + "/cancel", null, c.token());
        assertEquals(200, cancelled.status(), cancelled::toString);
        assertEquals("CANCELLED", cancelled.json().getString("bookingStatus"));
        assertEquals("REFUNDED", cancelled.json().getString("paymentStatus"));
        assertEquals(new BigDecimal("5000.00"), balanceOf(c));
    }

    @Test
    void handOverAndReturnGiveTheDepositBack() {
        TestCustomer c = readyCustomer("3000.00");
        var car = newCar("200.00", "800.00");
        LocalDate today = LocalDate.now();
        String id = book(c, car, today, today.plusDays(2)).json().getString("bookingId");
        assertEquals(200, Http.post("/bookings/" + id + "/pay", null, c.token()).status());
        assertEquals(new BigDecimal("1800.00"), balanceOf(c));        // 2 x 200 + 800 deposit

        String admin = adminToken();
        assertEquals("ACTIVE", Http.post("/bookings/" + id + "/pick-up", null, admin).json().getString("bookingStatus"));
        assertEquals(400, Http.post("/bookings/" + id + "/cancel", null, c.token()).status());
        assertEquals("COMPLETED", Http.post("/bookings/" + id + "/complete", null, admin).json().getString("bookingStatus"));
        assertEquals(new BigDecimal("2600.00"), balanceOf(c));        // deposit back, rental kept
    }

    @Test
    void aPaidBookingComesWithAPdf() {
        TestCustomer c = readyCustomer("5000.00");
        var car = newCar("250.00", "500.00");
        LocalDate start = LocalDate.now().plusDays(40);
        String id = book(c, car, start, start.plusDays(1)).json().getString("bookingId");
        Http.post("/bookings/" + id + "/pay", null, c.token());
        Http.Response pdf = Http.download("/bookings/" + id + "/pdf", c.token());
        assertEquals(200, pdf.status());
        assertTrue(pdf.body().startsWith("%PDF"));
    }

    @Test
    void passwordResetWithACodeFromTheEmail() {
        TestCustomer c = register();
        assertEquals(200, Http.post("/customers/password-reset-code",
                Json.createObjectBuilder().add("email", c.email()).build(), null).status());
        String code = codeFromLastEmail(c.email(), "reset");
        var reset = Json.createObjectBuilder().add("email", c.email()).add("code", code)
                .add("newPassword", "another2027").build();
        assertEquals(200, Http.put("/customers/reset-password", reset, null).status());
        assertFalse(login(c.email(), "another2027").isBlank());
        // A code works only once
        assertEquals(400, Http.put("/customers/reset-password", reset, null).status());
    }
}
