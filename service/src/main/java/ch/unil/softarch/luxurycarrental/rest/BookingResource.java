package ch.unil.softarch.luxurycarrental.rest;

import ch.unil.softarch.luxurycarrental.domain.dto.BookingRequest;
import ch.unil.softarch.luxurycarrental.domain.dto.ReasonRequest;
import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.security.Caller;
import ch.unil.softarch.luxurycarrental.security.LoggedIn;
import ch.unil.softarch.luxurycarrental.service.BookingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

/**
 * Bookings and their workflow. A booking changes only through these actions:
 * create → pay → pick-up → complete, or cancel / reject. There is no free-form update.
 * Customers see and act on their own bookings; pick-up, completion and rejection are for administrators.
 */
@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    @Inject
    private BookingService bookings;

    @Context
    private SecurityContext security;

    @GET
    public List<Booking> getAll(@QueryParam("status") BookingStatus status) {
        return bookings.getAll(status);
    }

    @GET
    @Path("/{id}")
    @LoggedIn
    public Booking get(@PathParam("id") UUID id) {
        return owned(id);
    }

    @GET
    @Path("/customer/{customerId}")
    @LoggedIn
    public List<Booking> getByCustomer(@PathParam("customerId") UUID customerId) {
        Caller.of(security).requireSelfOrAdmin(customerId);
        return bookings.getByCustomer(customerId);
    }

    @GET
    @Path("/car/{carId}")
    public List<Booking> getByCar(@PathParam("carId") UUID carId) {
        return bookings.getByCar(carId);
    }

    /** Customers book for themselves; administrators may book for any customer. */
    @POST
    @LoggedIn
    public Booking create(BookingRequest request) {
        Caller caller = Caller.of(security);
        if (request != null && request.customerId == null && !caller.isAdmin()) {
            request.customerId = caller.id();
        }
        caller.requireSelfOrAdmin(request == null ? null : request.customerId);
        return bookings.create(request);
    }

    @POST
    @Path("/{id}/pay")
    @LoggedIn
    public Booking pay(@PathParam("id") UUID id) {
        owned(id);
        return bookings.pay(id);
    }

    @POST
    @Path("/{id}/cancel")
    @LoggedIn
    public Booking cancel(@PathParam("id") UUID id) {
        owned(id);
        return bookings.cancel(id);
    }

    @POST
    @Path("/{id}/reject")
    public Booking reject(@PathParam("id") UUID id, ReasonRequest request) {
        return bookings.reject(id, request == null ? null : request.reason);
    }

    @POST
    @Path("/{id}/pick-up")
    public Booking pickUp(@PathParam("id") UUID id) {
        return bookings.pickUp(id);
    }

    @POST
    @Path("/{id}/complete")
    public Booking complete(@PathParam("id") UUID id) {
        return bookings.complete(id);
    }

    @GET
    @Path("/{id}/pdf")
    @LoggedIn
    @Produces("application/pdf")
    public Response pdf(@PathParam("id") UUID id) {
        Booking booking = owned(id);
        return Response.ok(bookings.pdf(id))
                .header("Content-Disposition", "attachment; filename=\"Booking-" + booking.getReference() + ".pdf\"")
                .build();
    }

    /** The booking, if the caller is its customer or an administrator. */
    private Booking owned(UUID id) {
        Booking booking = bookings.get(id);
        Caller.of(security).requireSelfOrAdmin(booking.getCustomer().getId());
        return booking;
    }
}
