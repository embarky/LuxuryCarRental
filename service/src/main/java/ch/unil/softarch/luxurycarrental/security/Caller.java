package ch.unil.softarch.luxurycarrental.security;

import ch.unil.softarch.luxurycarrental.api.Errors;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.UUID;

/** The authenticated user of the current API request, read from its token. */
public record Caller(Role role, UUID id) implements Principal {

    @Override
    public String getName() {
        return role + ":" + id;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /** The caller of the request, or 401 if the request carries no valid token. */
    public static Caller of(SecurityContext context) {
        if (context != null && context.getUserPrincipal() instanceof Caller caller) {
            return caller;
        }
        throw Errors.unauthorized("Please log in.");
    }

    /** Customers may only act on their own data; administrators on everyone's. */
    public void requireSelfOrAdmin(UUID customerId) {
        if (!isAdmin() && !(role == Role.CUSTOMER && id.equals(customerId))) {
            throw Errors.forbidden("You can only access your own account.");
        }
    }
}
