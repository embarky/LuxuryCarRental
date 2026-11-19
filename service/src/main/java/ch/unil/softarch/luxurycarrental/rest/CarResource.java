package ch.unil.softarch.luxurycarrental.rest;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.dto.BookedPeriod;
import ch.unil.softarch.luxurycarrental.domain.dto.MessageResponse;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.security.Public;
import ch.unil.softarch.luxurycarrental.service.CarService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * The fleet. Searching and reading are public; the full list (including cars in maintenance)
 * and all changes are for administrators.
 */
@Path("/cars")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarResource {

    @Inject
    private CarService cars;

    /** Every car, including those in maintenance. */
    @GET
    public List<Car> getAll() {
        return cars.getAll();
    }

    /**
     * Cars customers can book. Example:
     * {@code GET /cars/search?q=porsche&category=Sport&maxPrice=900&start=2026-07-01&end=2026-07-05}
     */
    @GET
    @Path("/search")
    @Public
    public List<Car> search(@QueryParam("q") String text,
                            @QueryParam("carTypeId") UUID carTypeId,
                            @QueryParam("category") String category,
                            @QueryParam("minPrice") BigDecimal minPrice,
                            @QueryParam("maxPrice") BigDecimal maxPrice,
                            @QueryParam("start") String start,
                            @QueryParam("end") String end) {
        return cars.search(text, carTypeId, category, minPrice, maxPrice, date(start), date(end));
    }

    @GET
    @Path("/{id}")
    @Public
    public Car get(@PathParam("id") UUID id) {
        return cars.get(id);
    }

    /** Dates on which the car is already taken. */
    @GET
    @Path("/{id}/booked")
    @Public
    public List<BookedPeriod> booked(@PathParam("id") UUID id) {
        return cars.bookedPeriods(id);
    }

    @POST
    public Car add(Car car) {
        return cars.add(car);
    }

    @PUT
    @Path("/{id}")
    public Car update(@PathParam("id") UUID id, Car update) {
        return cars.update(id, update);
    }

    @DELETE
    @Path("/{id}")
    public MessageResponse remove(@PathParam("id") UUID id) {
        cars.remove(id);
        return new MessageResponse("Car deleted.");
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new DomainException("Dates must be written as YYYY-MM-DD.");
        }
    }
}
