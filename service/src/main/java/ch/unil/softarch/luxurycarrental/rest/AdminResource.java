package ch.unil.softarch.luxurycarrental.rest;

import ch.unil.softarch.luxurycarrental.domain.dto.*;
import ch.unil.softarch.luxurycarrental.domain.entities.Admin;
import ch.unil.softarch.luxurycarrental.security.Caller;
import ch.unil.softarch.luxurycarrental.security.Public;
import ch.unil.softarch.luxurycarrental.security.Role;
import ch.unil.softarch.luxurycarrental.security.TokenService;
import ch.unil.softarch.luxurycarrental.service.AdminService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;

/** Administrator accounts. Everything except login and password reset is for administrators. */
@Path("/admins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    @Inject
    private AdminService admins;

    @Inject
    private TokenService tokens;

    @Context
    private SecurityContext security;

    @POST
    @Path("/login")
    @Public
    public AdminSession login(LoginRequest request) {
        Admin admin = admins.authenticate(request == null ? null : request.email,
                request == null ? null : request.password);
        AdminSession session = new AdminSession();
        session.token = tokens.issue(Role.ADMIN, admin.getId());
        session.admin = admin;
        return session;
    }

    @GET
    public List<Admin> getAll() {
        return admins.getAll();
    }

    @GET
    @Path("/{id}")
    public Admin get(@PathParam("id") UUID id) {
        return admins.get(id);
    }

    @POST
    public Admin create(AdminRequest request) {
        return admins.create(request);
    }

    @DELETE
    @Path("/{id}")
    public MessageResponse remove(@PathParam("id") UUID id) {
        admins.remove(id, Caller.of(security).id());
        return new MessageResponse("Administrator deleted.");
    }

    @PUT
    @Path("/me/password")
    public MessageResponse changeOwnPassword(PasswordChange change) {
        admins.changePassword(Caller.of(security).id(), change);
        return new MessageResponse("Your password has been changed.");
    }

    @POST
    @Path("/password-reset-code")
    @Public
    public MessageResponse sendResetCode(PasswordReset request) {
        admins.sendPasswordResetCode(request == null ? null : request.email);
        return new MessageResponse("If an account exists for this email, a code has been sent to it.");
    }

    @PUT
    @Path("/reset-password")
    @Public
    public MessageResponse resetPassword(PasswordReset request) {
        if (request == null) {
            throw new BadRequestException();
        }
        admins.resetPassword(request.email, request.code, request.newPassword);
        return new MessageResponse("Your password has been reset. You can now log in.");
    }
}
