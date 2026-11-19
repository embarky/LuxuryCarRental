package ch.unil.softarch.luxurycarrental.ui;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Protects pages that need a login.
 * <ul>
 *     <li>{@code /pages/admin/*} requires a logged-in administrator.</li>
 *     <li>{@code /pages/orders.xhtml} requires a logged-in customer.</li>
 * </ul>
 * Visitors without a login are sent to the login page; the page they asked for is
 * remembered so that {@link LoginRedirect} can bring them back after logging in.
 */
@WebFilter(urlPatterns = {"/pages/admin/*", "/pages/orders.xhtml"})
public class AuthenticationFilter implements Filter {

    private static final String LOGIN_PAGE = "/pages/login.xhtml";

    @Inject
    private AdminSessionBean adminSession;

    @Inject
    private CustomerSessionBean customerSession;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean adminArea = path.startsWith("/pages/admin/");
        boolean allowed = adminArea ? adminSession.isLoggedIn() : customerSession.isLoggedIn();

        if (allowed) {
            chain.doFilter(request, response);
            return;
        }

        // Remember the requested page (only for normal page loads, not form posts)
        if ("GET".equals(request.getMethod())) {
            String target = path + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
            request.getSession().setAttribute(
                    adminArea ? LoginRedirect.ADMIN_TARGET : LoginRedirect.CUSTOMER_TARGET, target);
        }

        String loginUrl = request.getContextPath() + LOGIN_PAGE + (adminArea ? "?role=admin" : "");

        if ("partial/ajax".equals(request.getHeader("Faces-Request"))) {
            // An Ajax request after the session expired: tell the Faces client to redirect
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().printf(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<partial-response><redirect url=\"%s\"></redirect></partial-response>",
                    loginUrl);
        } else {
            response.sendRedirect(loginUrl);
        }
    }
}
