package ch.unil.softarch.luxurycarrental.rest;

import ch.unil.softarch.luxurycarrental.domain.dto.MessageResponse;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import ch.unil.softarch.luxurycarrental.security.Public;
import ch.unil.softarch.luxurycarrental.service.CarTypeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

/** Car models. Reading is public, changes are for administrators. */
@Path("/cartypes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarTypeResource {

    @Inject
    private CarTypeService carTypes;

    @GET
    @Public
    public List<CarType> getAll() {
        return carTypes.getAll();
    }

    @GET
    @Path("/categories")
    @Public
    public List<String> getCategories() {
        return carTypes.getCategories();
    }

    @GET
    @Path("/{id}")
    @Public
    public CarType get(@PathParam("id") UUID id) {
        return carTypes.get(id);
    }

    @POST
    public CarType add(CarType carType) {
        return carTypes.add(carType);
    }

    @PUT
    @Path("/{id}")
    public CarType update(@PathParam("id") UUID id, CarType update) {
        return carTypes.update(id, update);
    }

    @DELETE
    @Path("/{id}")
    public MessageResponse remove(@PathParam("id") UUID id) {
        carTypes.remove(id);
        return new MessageResponse("Car type deleted.");
    }
}
