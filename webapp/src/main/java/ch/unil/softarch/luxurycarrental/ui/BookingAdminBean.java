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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Admin list of all bookings, with the actions allowed in each state. */
@Named("bookingAdminBean")
@ViewScoped
public class BookingAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private BookingStatus status;
    private String text;
    private List<Booking> bookings = Collections.emptyList();

    private UUID rejecting;
    private String reason;

    @PostConstruct
    public void init() {
        load();
    }

    public void load() {
        try {
            List<Booking> all = api.bookings(status);
            if (text != null && !text.isBlank()) {
                String q = text.trim().toLowerCase(Locale.ROOT);
                all = all.stream().filter(b -> b.getReference().toLowerCase(Locale.ROOT).contains(q)
                        || b.getCustomer().getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || b.getCustomer().getEmail().toLowerCase(Locale.ROOT).contains(q)
                        || b.getCar().getDisplayName().toLowerCase(Locale.ROOT).contains(q)
                        || b.getCar().getLicensePlate().toLowerCase(Locale.ROOT).contains(q)).toList();
            }
            bookings = all;
        } catch (ApiException e) {
            bookings = Collections.emptyList();
            Messages.error(e);
        }
    }

    public void pickUp(UUID id) {
        run(() -> api.pickUp(id), "Car handed over – the booking is now in progress.");
    }

    public void complete(UUID id) {
        run(() -> api.complete(id), "Car returned – the deposit has been refunded to the customer.");
    }

    public void startReject(UUID id) {
        rejecting = id;
        reason = null;
    }

    public void cancelReject() {
        rejecting = null;
        reason = null;
    }

    public void confirmReject() {
        UUID id = rejecting;
        run(() -> api.reject(id, reason), "Booking rejected. The customer has been informed and refunded if they had paid.");
        rejecting = null;
        reason = null;
    }

    private void run(Runnable action, String success) {
        try {
            action.run();
            Messages.info(success);
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public boolean canPickUp(Booking b) {
        return b.getBookingStatus() == BookingStatus.CONFIRMED && !b.getStartDate().isAfter(LocalDate.now());
    }

    public boolean canComplete(Booking b) {
        return b.getBookingStatus() == BookingStatus.ACTIVE;
    }

    public boolean canReject(Booking b) {
        return b.getBookingStatus() == BookingStatus.PENDING || b.getBookingStatus() == BookingStatus.CONFIRMED;
    }

    public boolean isRejecting(Booking b) {
        return b.getBookingId().equals(rejecting);
    }

    public BookingStatus[] getStatuses() { return BookingStatus.values(); }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<Booking> getBookings() { return bookings; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
