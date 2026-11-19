package ch.unil.softarch.luxurycarrental.domain.entities;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.domain.enums.PaymentStatus;
import ch.unil.softarch.luxurycarrental.domain.model.DateRange;
import ch.unil.softarch.luxurycarrental.domain.model.Money;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A rental of one car by one customer for a period.
 * <p>
 * The booking owns its life cycle: every transition is a method that checks the current
 * state, moves the money between the customer's balance and the company, and returns the
 * amount refunded where that applies. Status and payment status have no public setters
 * for business code, so the rules cannot be bypassed.
 * </p>
 * <pre>
 * PENDING ──pay──▶ CONFIRMED ──pickUp──▶ ACTIVE ──complete──▶ COMPLETED
 * PENDING ──cancel / reject / expire──▶ CANCELLED / REJECTED / EXPIRED   (nothing to refund)
 * CONFIRMED ──cancel / reject──▶ CANCELLED / REJECTED                    (rental + deposit refunded)
 * </pre>
 */
@Entity
@Table(name = "booking")
public class Booking implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How long a customer has to pay before an unpaid booking frees the car. */
    public static final Duration PAYMENT_WINDOW = Duration.ofHours(24);

    /** A paid booking can be cancelled free of charge until this long before pick-up. */
    public static final Duration FREE_CANCELLATION = Duration.ofHours(48);

    @Id
    @Column(length = 36)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** Rental price for the whole period (days × daily rate at booking time). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    /** Refundable deposit, charged together with the rental. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime closedAt;

    @Column(length = 500)
    private String rejectionReason;

    @Version
    private long version;

    /** For JPA and JSON. Use {@link #create} in business code. */
    public Booking() {
    }

    // -------------------------------------------------------------------------
    // Creation
    // -------------------------------------------------------------------------

    /**
     * Creates a pending booking after checking the rules that only depend on the booking
     * itself. That the dates are free is checked by the service, which sees the other bookings.
     */
    public static Booking create(Customer customer, Car car, DateRange period, LocalDateTime now) {
        if (customer == null || car == null) {
            throw new DomainException("Customer and car are required.");
        }
        if (!car.isInService()) {
            throw new DomainException("This car is currently out of service.");
        }
        if (!period.startsOnOrAfter(now.toLocalDate())) {
            throw new DomainException("The pick-up date cannot be in the past.");
        }
        if (!customer.hasValidLicenceOn(period.end())) {
            throw new DomainException("Your driving licence must be valid until the return date ("
                    + period.end() + "). Please update it in your account.");
        }

        Booking booking = new Booking();
        booking.customer = customer;
        booking.car = car;
        booking.startDate = period.start();
        booking.endDate = period.end();
        booking.totalCost = Money.of(car.getDailyRentalPrice().multiply(BigDecimal.valueOf(period.days())));
        booking.depositAmount = Money.of(car.getDepositAmount());
        booking.bookingStatus = BookingStatus.PENDING;
        booking.paymentStatus = PaymentStatus.UNPAID;
        booking.createdAt = now;
        return booking;
    }

    @PrePersist
    protected void onCreate() {
        if (bookingId == null) {
            bookingId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // -------------------------------------------------------------------------
    // Transitions
    // -------------------------------------------------------------------------

    /** Customer pays rental + deposit from their balance: PENDING → CONFIRMED. */
    public void pay(LocalDateTime now) {
        requireStatus(BookingStatus.PENDING, "Only an unpaid booking can be paid.");
        if (isPaymentOverdue(now)) {
            throw new DomainException("The payment window has passed; this booking has expired.");
        }
        if (!customer.isEmailVerified()) {
            throw new DomainException("Please confirm your email address before paying. "
                    + "Enter the code we sent you on your account page.");
        }
        if (!customer.isVerifiedIdentity()) {
            throw new DomainException("Your identity has not been verified yet. "
                    + "Our team checks your driving licence before your first payment.");
        }
        customer.debit(getAmountDue());
        bookingStatus = BookingStatus.CONFIRMED;
        paymentStatus = PaymentStatus.PAID;
        paidAt = now;
    }

    /**
     * Customer cancels. An unpaid booking is cancelled at no cost; a paid one is refunded in
     * full until {@link #FREE_CANCELLATION} before pick-up.
     *
     * @return the amount refunded to the customer
     */
    public BigDecimal cancel(LocalDateTime now) {
        if (bookingStatus == BookingStatus.PENDING) {
            close(BookingStatus.CANCELLED, now);
            return Money.ZERO;
        }
        requireStatus(BookingStatus.CONFIRMED, "This booking can no longer be cancelled.");
        LocalDateTime deadline = startDate.atStartOfDay().minus(FREE_CANCELLATION);
        if (now.isAfter(deadline)) {
            throw new DomainException("Paid bookings can only be cancelled up to "
                    + FREE_CANCELLATION.toHours() + " hours before pick-up. Please contact us.");
        }
        return refundAndClose(BookingStatus.CANCELLED, now);
    }

    /**
     * Administrator refuses the booking (e.g. licence could not be verified). A paid
     * booking is refunded in full.
     *
     * @return the amount refunded to the customer
     */
    public BigDecimal reject(String reason, LocalDateTime now) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("Please give a reason for the rejection.");
        }
        if (bookingStatus != BookingStatus.PENDING && bookingStatus != BookingStatus.CONFIRMED) {
            throw new DomainException("Only pending or confirmed bookings can be rejected.");
        }
        rejectionReason = reason.trim();
        if (bookingStatus == BookingStatus.PENDING) {
            close(BookingStatus.REJECTED, now);
            return Money.ZERO;
        }
        return refundAndClose(BookingStatus.REJECTED, now);
    }

    /** The customer collects the car: CONFIRMED → ACTIVE, not before the pick-up date. */
    public void pickUp(LocalDate today) {
        requireStatus(BookingStatus.CONFIRMED, "Only a paid booking can be picked up.");
        if (today.isBefore(startDate)) {
            throw new DomainException("The car can be picked up from " + startDate + ".");
        }
        bookingStatus = BookingStatus.ACTIVE;
    }

    /**
     * The car is back: ACTIVE → COMPLETED. The deposit is returned to the customer.
     *
     * @return the deposit refunded
     */
    public BigDecimal complete(LocalDateTime now) {
        requireStatus(BookingStatus.ACTIVE, "Only a booking in progress can be completed.");
        customer.credit(depositAmount);
        close(BookingStatus.COMPLETED, now);
        return depositAmount;
    }

    /**
     * Frees the car when an unpaid booking was not paid in time.
     *
     * @return true if the booking expired now
     */
    public boolean expireIfOverdue(LocalDateTime now) {
        if (bookingStatus == BookingStatus.PENDING && isPaymentOverdue(now)) {
            close(BookingStatus.EXPIRED, now);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Rental + deposit: what the customer pays to confirm. */
    public BigDecimal getAmountDue() {
        return Money.of(totalCost.add(depositAmount));
    }

    @JsonbTransient
    public DateRange getPeriod() {
        return new DateRange(startDate, endDate);
    }

    public long getDays() {
        return startDate == null || endDate == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }

    /** Time by which a pending booking must be paid. */
    public LocalDateTime getPaymentDeadline() {
        return createdAt == null ? null : createdAt.plus(PAYMENT_WINDOW);
    }

    public boolean isPaymentOverdue(LocalDateTime now) {
        return getPaymentDeadline() != null && now.isAfter(getPaymentDeadline());
    }

    public boolean isOccupyingCar() {
        return bookingStatus != null && bookingStatus.blocksCar();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BigDecimal refundAndClose(BookingStatus finalStatus, LocalDateTime now) {
        BigDecimal refund = getAmountDue();
        customer.credit(refund);
        paymentStatus = PaymentStatus.REFUNDED;
        close(finalStatus, now);
        return refund;
    }

    private void close(BookingStatus finalStatus, LocalDateTime now) {
        bookingStatus = finalStatus;
        closedAt = now;
    }

    private void requireStatus(BookingStatus expected, String message) {
        if (bookingStatus != expected) {
            throw new DomainException(message + " (current status: " + bookingStatus + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Getters (and setters needed by JPA / JSON-B)
    // -------------------------------------------------------------------------

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BookingStatus getBookingStatus() { return bookingStatus; }
    /** For JSON-B (reading API responses). Business code uses the transition methods. */
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    /** For JSON-B (reading API responses). Business code uses the transition methods. */
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    /** Short reference shown to customers, e.g. "A1B2C3D4". */
    public String getReference() {
        return bookingId == null ? "-" : bookingId.toString().substring(0, 8).toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Booking other && bookingId != null && bookingId.equals(other.bookingId));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bookingId);
    }

    @Override
    public String toString() {
        return "Booking{" + getReference() + ", " + startDate + " → " + endDate + ", " + bookingStatus + "}";
    }
}
