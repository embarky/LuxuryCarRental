package ch.unil.softarch.luxurycarrental.api;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turns every exception into a JSON error with the right status code:
 * <ul>
 *     <li>{@link DomainException} → 400, or 409 for conflicts (rule broken by the request)</li>
 *     <li>{@link WebApplicationException} → its own status (404, 403, ...)</li>
 *     <li>{@link OptimisticLockException} → 409 (someone changed the data at the same time)</li>
 *     <li>anything else → 500 with a generic message; the details go to the server log only</li>
 * </ul>
 * Transactions wrap exceptions, so the whole cause chain is searched.
 */
@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErrorMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        for (Throwable t = exception; t != null; t = t.getCause()) {
            if (t instanceof DomainException e) {
                return Errors.response(e.isConflict() ? 409 : 400, e.getMessage());
            }
            if (t instanceof WebApplicationException e) {
                int status = e.getResponse().getStatus();
                if (e.getResponse().hasEntity()) {
                    return e.getResponse();
                }
                return Errors.response(status, status == 404 ? "Not found." : e.getMessage());
            }
            if (t instanceof OptimisticLockException) {
                return Errors.response(409, "This record was changed by someone else at the same time. Please try again.");
            }
            if (t.getCause() == t) {
                break;
            }
        }
        LOG.log(Level.SEVERE, "Unexpected error in the API", exception);
        return Errors.response(500, "An unexpected error occurred. Please try again later.");
    }
}
