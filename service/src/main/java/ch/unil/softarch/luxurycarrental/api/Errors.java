package ch.unil.softarch.luxurycarrental.api;

import ch.unil.softarch.luxurycarrental.domain.dto.ApiError;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Builds API errors with a JSON body {@code {"status": ..., "message": ...}}. */
public final class Errors {

    private Errors() {
    }

    public static WebApplicationException badRequest(String message) {
        return of(Response.Status.BAD_REQUEST, message);
    }

    public static WebApplicationException unauthorized(String message) {
        return of(Response.Status.UNAUTHORIZED, message);
    }

    public static WebApplicationException forbidden(String message) {
        return of(Response.Status.FORBIDDEN, message);
    }

    public static WebApplicationException notFound(String message) {
        return of(Response.Status.NOT_FOUND, message);
    }

    public static WebApplicationException conflict(String message) {
        return of(Response.Status.CONFLICT, message);
    }

    public static Response response(int status, String message) {
        return Response.status(status)
                .entity(new ApiError(status, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static WebApplicationException of(Response.Status status, String message) {
        return new WebApplicationException(message, response(status.getStatusCode(), message));
    }
}
