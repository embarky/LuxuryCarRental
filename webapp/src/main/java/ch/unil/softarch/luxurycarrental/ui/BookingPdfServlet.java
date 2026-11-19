package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.client.ApiTokens;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.UUID;

/**
 * Downloads a booking confirmation PDF ({@code /booking-pdf?id=...}). The API checks that the
 * logged-in user owns the booking (or is an administrator).
 */
@WebServlet("/booking-pdf")
public class BookingPdfServlet extends HttpServlet {

    @Inject
    private Api api;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object token = session == null ? null : session.getAttribute(ApiTokens.CUSTOMER);
        if (token == null && session != null) {
            token = session.getAttribute(ApiTokens.ADMIN);
        }
        if (token == null) {
            response.sendRedirect(request.getContextPath() + "/pages/login.xhtml");
            return;
        }
        try {
            UUID id = UUID.fromString(request.getParameter("id"));
            byte[] pdf = api.bookingPdf(id, token.toString());
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=\"Booking-"
                    + id.toString().substring(0, 8).toUpperCase() + ".pdf\"");
            response.setContentLength(pdf.length);
            response.getOutputStream().write(pdf);
        } catch (IllegalArgumentException | NullPointerException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ApiException e) {
            response.sendError(e.getStatus(), e.getMessage());
        }
    }
}
