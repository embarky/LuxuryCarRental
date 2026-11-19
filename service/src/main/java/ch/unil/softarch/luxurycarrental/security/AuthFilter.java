package ch.unil.softarch.luxurycarrental.security;

import ch.unil.softarch.luxurycarrental.domain.dto.ApiError;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Optional;

/**
 * Authenticates every API request from its {@code Authorization: Bearer <token>} header and
 * enforces the access level of the resource method:
 * {@link Public} → anyone, {@link LoggedIn} → any logged-in user, otherwise administrators only.
 * Deny by default: a new endpoint is admin-only until it is marked otherwise.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    private TokenService tokens;

    @Override
    public void filter(ContainerRequestContext request) {
        Optional<Caller> caller = Optional.empty();
        String header = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            caller = tokens.verify(header.substring(7).trim());
        }
        caller.ifPresent(c -> request.setSecurityContext(new CallerContext(c, request.getSecurityContext())));

        Method method = resourceInfo.getResourceMethod();
        Class<?> type = resourceInfo.getResourceClass();
        if (method == null || is(method, type, Public.class)) {
            return;
        }
        if (caller.isEmpty()) {
            abort(request, Response.Status.UNAUTHORIZED, "Please log in.");
        } else if (!is(method, type, LoggedIn.class) && !caller.get().isAdmin()) {
            abort(request, Response.Status.FORBIDDEN, "Administrators only.");
        }
    }

    private static boolean is(Method method, Class<?> type, Class<? extends java.lang.annotation.Annotation> a) {
        return method.isAnnotationPresent(a) || (type != null && type.isAnnotationPresent(a));
    }

    private static void abort(ContainerRequestContext request, Response.Status status, String message) {
        request.abortWith(Response.status(status)
                .entity(new ApiError(status.getStatusCode(), message))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

    /** Exposes the caller as the request's user principal. */
    private record CallerContext(Caller caller, SecurityContext original) implements SecurityContext {
        @Override
        public Principal getUserPrincipal() {
            return caller;
        }

        @Override
        public boolean isUserInRole(String role) {
            return caller.role().name().equals(role);
        }

        @Override
        public boolean isSecure() {
            return original != null && original.isSecure();
        }

        @Override
        public String getAuthenticationScheme() {
            return "Bearer";
        }
    }
}
