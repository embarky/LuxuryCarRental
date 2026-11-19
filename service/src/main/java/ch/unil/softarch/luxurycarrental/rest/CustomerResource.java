package ch.unil.softarch.luxurycarrental.rest;

import ch.unil.softarch.luxurycarrental.domain.dto.*;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import ch.unil.softarch.luxurycarrental.security.Caller;
import ch.unil.softarch.luxurycarrental.security.LoggedIn;
import ch.unil.softarch.luxurycarrental.security.Public;
import ch.unil.softarch.luxurycarrental.security.Role;
import ch.unil.softarch.luxurycarrental.security.TokenService;
import ch.unil.softarch.luxurycarrental.service.CustomerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

/**
 * Customer accounts.
 * Public: register, login, password reset. Customers: their own account. Administrators: all accounts.
 */
@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @Inject
    private CustomerService customers;

    @Inject
    private TokenService tokens;

    @Context
    private SecurityContext security;

    @POST
    @Public
    public Customer register(RegistrationRequest request) {
        return customers.register(request);
    }

    @POST
    @Path("/login")
    @Public
    public CustomerSession login(LoginRequest request) {
        Customer customer = customers.authenticate(request == null ? null : request.email,
                request == null ? null : request.password);
        CustomerSession session = new CustomerSession();
        session.token = tokens.issue(Role.CUSTOMER, customer.getId());
        session.customer = customer;
        return session;
    }

    @GET
    public List<Customer> getAll() {
        return customers.getAll();
    }

    @GET
    @Path("/{id}")
    @LoggedIn
    public Customer get(@PathParam("id") UUID id) {
        Caller.of(security).requireSelfOrAdmin(id);
        return customers.get(id);
    }

    @PUT
    @Path("/{id}")
    @LoggedIn
    public Customer updateProfile(@PathParam("id") UUID id, ProfileUpdate update) {
        Caller.of(security).requireSelfOrAdmin(id);
        return customers.updateProfile(id, update);
    }

    @POST
    @Path("/{id}/top-up")
    @LoggedIn
    public Customer topUp(@PathParam("id") UUID id, AmountRequest request) {
        Caller.of(security).requireSelfOrAdmin(id);
        return customers.topUp(id, request == null ? null : request.amount);
    }

    @PUT
    @Path("/{id}/password")
    @LoggedIn
    public MessageResponse changePassword(@PathParam("id") UUID id, PasswordChange change) {
        Caller caller = Caller.of(security);
        caller.requireSelfOrAdmin(id);
        customers.changePassword(id, change);
        return new MessageResponse("Your password has been changed.");
    }

    @PUT
    @Path("/{id}/verified")
    public Customer setVerified(@PathParam("id") UUID id, @QueryParam("value") @DefaultValue("true") boolean verified) {
        return customers.setVerified(id, verified);
    }

    @DELETE
    @Path("/{id}")
    public MessageResponse remove(@PathParam("id") UUID id) {
        customers.remove(id);
        return new MessageResponse("Customer deleted.");
    }

    @POST
    @Path("/password-reset-code")
    @Public
    public MessageResponse sendResetCode(PasswordReset request) {
        customers.sendPasswordResetCode(request == null ? null : request.email);
        return new MessageResponse("If an account exists for this email, a code has been sent to it.");
    }

    @PUT
    @Path("/reset-password")
    @Public
    public MessageResponse resetPassword(PasswordReset request) {
        if (request == null) {
            throw new BadRequestException();
        }
        customers.resetPassword(request.email, request.code, request.newPassword);
        return new MessageResponse("Your password has been reset. You can now log in.");
    }
}
