package ch.unil.softarch.luxurycarrental.domain;

import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import ch.unil.softarch.luxurycarrental.domain.enums.PaymentStatus;
import ch.unil.softarch.luxurycarrental.domain.model.DateRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The money rules of a booking: whatever path a booking takes, the customer ends up
 * paying exactly the rental (or nothing), never more or less.
 * Example: 3 days at CHF 300 = CHF 900 rental, CHF 500 deposit, balance CHF 2000.
 */
class BookingTest {

    private static final LocalDateTime NOW = TestData.TODAY.atTime(10, 0);
    private static final DateRange JUNE_10_TO_13 =
            new DateRange(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13));

    private Customer customer;
    private Car car;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = TestData.customer("2000.00", true);
        car = TestData.car();
        booking = Booking.create(customer, car, JUNE_10_TO_13, NOW);
    }

    private BigDecimal balance() {
        return customer.getBalance();
    }

    // ---- creation ---------------------------------------------------------

    @Test
    void newBookingIsPendingAndChargesNothing() {
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());
        assertEquals(PaymentStatus.UNPAID, booking.getPaymentStatus());
        assertEquals(new BigDecimal("900.00"), booking.getTotalCost());
        assertEquals(new BigDecimal("500.00"), booking.getDepositAmount());
        assertEquals(new BigDecimal("1400.00"), booking.getAmountDue());
        assertEquals(new BigDecimal("2000.00"), balance());
    }

    @Test
    void cannotBookInThePast() {
        DateRange past = new DateRange(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 22));
        assertThrows(DomainException.class, () -> Booking.create(customer, car, past, NOW));
    }

    @Test
    void cannotBookACarInMaintenance() {
        car.setStatus(CarStatus.MAINTENANCE);
        assertThrows(DomainException.class, () -> Booking.create(customer, car, JUNE_10_TO_13, NOW));
    }

    @Test
    void licenceMustCoverTheWholeRental() {
        customer.setDrivingLicenseExpiryDate(LocalDate.of(2026, 6, 12));
        assertThrows(DomainException.class, () -> Booking.create(customer, car, JUNE_10_TO_13, NOW));
    }

    // ---- happy path -------------------------------------------------------

    @Test
    void fullRentalCostsExactlyTheRental() {
        booking.pay(NOW);
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(new BigDecimal("600.00"), balance());       // 2000 - 900 - 500

        booking.pickUp(LocalDate.of(2026, 6, 10));
        assertEquals(BookingStatus.ACTIVE, booking.getBookingStatus());

        assertEquals(new BigDecimal("500.00"), booking.complete(NOW.plusDays(12)));
        assertEquals(BookingStatus.COMPLETED, booking.getBookingStatus());
        assertEquals(new BigDecimal("1100.00"), balance());      // paid 900 in total
    }

    // ---- payment ----------------------------------------------------------

    @Test
    void cannotPayTwice() {
        booking.pay(NOW);
        assertThrows(DomainException.class, () -> booking.pay(NOW));
        assertEquals(new BigDecimal("600.00"), balance());
    }

    @Test
    void unverifiedCustomerCannotPay() {
        customer.setVerifiedIdentity(false);
        assertThrows(DomainException.class, () -> booking.pay(NOW));
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());
    }

    @Test
    void insufficientBalanceLeavesEverythingUnchanged() {
        customer.setBalance(new BigDecimal("1000.00"));
        assertThrows(DomainException.class, () -> booking.pay(NOW));
        assertEquals(BookingStatus.PENDING, booking.getBookingStatus());
        assertEquals(new BigDecimal("1000.00"), balance());
    }

    // ---- cancellation -----------------------------------------------------

    @Test
    void cancellingAnUnpaidBookingRefundsNothing() {
        assertEquals(new BigDecimal("0.00"), booking.cancel(NOW));
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        assertEquals(new BigDecimal("2000.00"), balance());      // old bug: +500 for free
    }

    @Test
    void cancellingAPaidBookingRefundsEverything() {
        booking.pay(NOW);
        assertEquals(new BigDecimal("1400.00"), booking.cancel(NOW));
        assertEquals(PaymentStatus.REFUNDED, booking.getPaymentStatus());
        assertEquals(new BigDecimal("2000.00"), balance());
    }

    @Test
    void paidBookingCannotBeCancelledShortlyBeforePickUp() {
        booking.pay(NOW);
        LocalDateTime dayBefore = LocalDate.of(2026, 6, 9).atTime(9, 0);
        assertThrows(DomainException.class, () -> booking.cancel(dayBefore));
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
    }

    @Test
    void finishedBookingsCannotBeCancelled() {
        booking.pay(NOW);
        booking.pickUp(LocalDate.of(2026, 6, 10));
        booking.complete(NOW.plusDays(12));
        assertThrows(DomainException.class, () -> booking.cancel(NOW.plusDays(13)));
        assertEquals(new BigDecimal("1100.00"), balance());
    }

    // ---- rejection --------------------------------------------------------

    @Test
    void rejectingAPaidBookingRefundsExactlyWhatWasPaid() {
        booking.pay(NOW);
        assertEquals(new BigDecimal("1400.00"), booking.reject("Licence could not be verified", NOW));
        assertEquals(BookingStatus.REJECTED, booking.getBookingStatus());
        assertEquals("Licence could not be verified", booking.getRejectionReason());
        assertEquals(new BigDecimal("2000.00"), balance());
    }

    @Test
    void rejectionNeedsAReason() {
        assertThrows(DomainException.class, () -> booking.reject(" ", NOW));
    }

    @Test
    void activeBookingCannotBeRejected() {
        booking.pay(NOW);
        booking.pickUp(LocalDate.of(2026, 6, 10));
        assertThrows(DomainException.class, () -> booking.reject("Too late", NOW));
    }

    // ---- pick-up, completion, expiry --------------------------------------

    @Test
    void cannotPickUpBeforeTheStartDate() {
        booking.pay(NOW);
        assertThrows(DomainException.class, () -> booking.pickUp(LocalDate.of(2026, 6, 9)));
    }

    @Test
    void onlyAnActiveBookingCanBeCompleted() {
        booking.pay(NOW);
        assertThrows(DomainException.class, () -> booking.complete(NOW));
        assertEquals(new BigDecimal("600.00"), balance());
    }

    @Test
    void unpaidBookingExpiresAfterThePaymentWindow() {
        assertFalse(booking.expireIfOverdue(NOW.plusHours(23)));
        assertTrue(booking.expireIfOverdue(NOW.plusHours(25)));
        assertEquals(BookingStatus.EXPIRED, booking.getBookingStatus());
        assertFalse(booking.isOccupyingCar());
    }

    @Test
    void paidBookingNeverExpires() {
        booking.pay(NOW);
        assertFalse(booking.expireIfOverdue(NOW.plusDays(30)));
    }
}
