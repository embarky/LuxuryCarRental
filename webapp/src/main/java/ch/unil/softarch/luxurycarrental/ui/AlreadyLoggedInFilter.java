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
 * Sends visitors who are already logged in away from the login and sign-up pages:
 * customers to their account, administrators to the dashboard.
 */
@WebFilter(urlPatterns = {"/pages/login.xhtml", "/pages/register.xhtml", "/pages/staff-login.xhtml"})
public class AlreadyLoggedInFilter implements Filter {

    @Inject
    private AdminSessionBean adminSession;

    @Inject
    private CustomerSessionBean customerSession;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if ("GET".equals(request.getMethod())) {
            boolean staffLogin = request.getRequestURI().endsWith("/staff-login.xhtml");
            if (staffLogin && adminSession.isLoggedIn()) {
                response.sendRedirect(request.getContextPath() + "/pages/admin/dashboard.xhtml");
                return;
            }
            if (!staffLogin && customerSession.isLoggedIn()) {
                response.sendRedirect(request.getContextPath() + "/pages/account.xhtml");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
