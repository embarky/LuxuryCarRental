package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** "My bookings": the customer's own bookings, with payment and cancellation. */
@Named("orderBean")
@ViewScoped
public class OrderBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    @Inject
    private CustomerSessionBean session;

    private List<Booking> bookings = Collections.emptyList();

    @PostConstruct
    public void init() {
        load();
    }

    private void load() {
        try {
            session.refresh();
            bookings = api.bookingsOfCustomer(session.getCustomer().getId());
        } catch (ApiException e) {
            bookings = Collections.emptyList();
            Messages.error(e);
        }
    }

    public void pay(UUID bookingId) {
        try {
            Booking b = api.pay(bookingId);
            Messages.info("Payment received – your " + b.getCar().getDisplayName() + " is confirmed.");
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public void cancel(UUID bookingId) {
        try {
            api.cancel(bookingId);
            Messages.info("Your booking has been cancelled.");
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public boolean canPay(Booking b) {
        return b.getBookingStatus() == BookingStatus.PENDING;
    }

    public boolean canCancel(Booking b) {
        return b.getBookingStatus() == BookingStatus.PENDING
                || (b.getBookingStatus() == BookingStatus.CONFIRMED
                && LocalDateTime.now().isBefore(b.getStartDate().atStartOfDay().minusHours(48)));
    }

    public boolean isUpcoming(Booking b) {
        return b.getBookingStatus() != null && !b.getBookingStatus().isFinal() && !b.getEndDate().isBefore(LocalDate.now());
    }

    public List<Booking> getBookings() { return bookings; }
}
