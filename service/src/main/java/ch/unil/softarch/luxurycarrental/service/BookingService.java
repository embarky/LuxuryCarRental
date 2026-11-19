package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.api.Errors;
import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.dto.BookingRequest;
import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.domain.model.DateRange;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking workflow. The rules of each step live in {@link Booking}; this service loads the
 * data, checks what needs the database (free dates, ownership), and sends the emails.
 */
@ApplicationScoped
public class BookingService {

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    @Inject
    private CarService cars;

    @Inject
    private Event<MailEvent> mail;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Booking get(UUID id) {
        Booking booking = em.find(Booking.class, id);
        if (booking == null) {
            throw Errors.notFound("Booking not found.");
        }
        return booking;
    }

    /** All bookings, newest first, optionally only those with the given status. */
    public List<Booking> getAll(BookingStatus status) {
        if (status == null) {
            return em.createQuery("SELECT b FROM Booking b ORDER BY b.createdAt DESC", Booking.class).getResultList();
        }
        return em.createQuery("SELECT b FROM Booking b WHERE b.bookingStatus = :s ORDER BY b.createdAt DESC", Booking.class)
                .setParameter("s", status)
                .getResultList();
    }

    public List<Booking> getByCustomer(UUID customerId) {
        return em.createQuery("SELECT b FROM Booking b WHERE b.customer.id = :id ORDER BY b.startDate DESC", Booking.class)
                .setParameter("id", customerId)
                .getResultList();
    }

    public List<Booking> getByCar(UUID carId) {
        return em.createQuery("SELECT b FROM Booking b WHERE b.car.id = :id ORDER BY b.startDate DESC", Booking.class)
                .setParameter("id", carId)
                .getResultList();
    }

    // -------------------------------------------------------------------------
    // Workflow
    // -------------------------------------------------------------------------

    /**
     * Creates a pending booking if the car is free for the whole period.
     * <p>
     * The car row is locked ({@code SELECT ... FOR UPDATE}) while the dates are checked, so two
     * customers booking the same car at the same moment are handled one after the other and
     * the second one gets a "dates already booked" error instead of a double booking.
     * </p>
     */
    @Transactional
    public Booking create(BookingRequest request) {
        if (request == null || request.customerId == null || request.carId == null) {
            throw new DomainException("Customer and car are required.");
        }
        Customer customer = em.find(Customer.class, request.customerId);
        if (customer == null) {
            throw Errors.notFound("Customer not found.");
        }
        Car car = em.find(Car.class, request.carId, LockModeType.PESSIMISTIC_WRITE);
        if (car == null) {
            throw Errors.notFound("Car not found.");
        }
        DateRange period = new DateRange(request.startDate, request.endDate);
        if (cars.overlappingBookings(car.getId(), period) > 0) {
            throw DomainException.conflict("This car is already booked for some of these dates. "
                    + "Please choose other dates.");
        }

        Booking booking = Booking.create(customer, car, period, LocalDateTime.now());
        em.persist(booking);
        em.flush();   // assigns the id used in the PDF and the email

        mail.fire(new MailEvent(customer.getEmail(), EmailTemplates.bookingCreated(booking),
                BookingPdf.create(booking), "Booking-" + booking.getReference() + ".pdf"));
        return booking;
    }

    @Transactional
    public Booking pay(UUID id) {
        Booking booking = get(id);
        booking.pay(LocalDateTime.now());
        mail.fire(new MailEvent(booking.getCustomer().getEmail(), EmailTemplates.bookingPaid(booking),
                BookingPdf.create(booking), "Booking-" + booking.getReference() + "-confirmed.pdf"));
        return booking;
    }

    @Transactional
    public Booking cancel(UUID id) {
        Booking booking = get(id);
        BigDecimal refund = booking.cancel(LocalDateTime.now());
        mail.fire(MailEvent.of(booking.getCustomer().getEmail(), EmailTemplates.bookingCancelled(booking, refund)));
        return booking;
    }

    @Transactional
    public Booking reject(UUID id, String reason) {
        Booking booking = get(id);
        BigDecimal refund = booking.reject(reason, LocalDateTime.now());
        mail.fire(MailEvent.of(booking.getCustomer().getEmail(),
                EmailTemplates.bookingRejected(booking, booking.getRejectionReason(), refund)));
        return booking;
    }

    @Transactional
    public Booking pickUp(UUID id) {
        Booking booking = get(id);
        booking.pickUp(LocalDateTime.now().toLocalDate());
        return booking;
    }

    @Transactional
    public Booking complete(UUID id) {
        Booking booking = get(id);
        BigDecimal deposit = booking.complete(LocalDateTime.now());
        mail.fire(MailEvent.of(booking.getCustomer().getEmail(), EmailTemplates.bookingCompleted(booking, deposit)));
        return booking;
    }

    /** Releases the cars of unpaid bookings whose payment window has passed. Returns how many expired. */
    @Transactional
    public int expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> pending = em.createQuery("SELECT b FROM Booking b WHERE b.bookingStatus = :s AND b.createdAt < :cutoff",
                        Booking.class)
                .setParameter("s", BookingStatus.PENDING)
                .setParameter("cutoff", now.minus(Booking.PAYMENT_WINDOW))
                .getResultList();
        int expired = 0;
        for (Booking booking : pending) {
            if (booking.expireIfOverdue(now)) {
                expired++;
                mail.fire(MailEvent.of(booking.getCustomer().getEmail(), EmailTemplates.bookingExpired(booking)));
            }
        }
        return expired;
    }

    public byte[] pdf(UUID id) {
        return BookingPdf.create(get(id));
    }
}
