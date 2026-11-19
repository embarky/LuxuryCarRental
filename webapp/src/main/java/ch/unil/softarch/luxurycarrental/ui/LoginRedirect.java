package ch.unil.softarch.luxurycarrental.ui;

import jakarta.faces.context.FacesContext;

import java.util.Map;

/**
 * Remembers the page a visitor wanted to open before being sent to the login page,
 * so that a successful login can bring them back to it.
 * <p>
 * Targets are stored in the HTTP session by the server (never taken from a URL
 * parameter), so a login can only redirect to a page of this application.
 * </p>
 */
public final class LoginRedirect {

    /** Session key for the page an admin wanted to open. */
    public static final String ADMIN_TARGET = "loginRedirect.admin";

    /** Session key for the page a customer wanted to open. */
    public static final String CUSTOMER_TARGET = "loginRedirect.customer";

    private LoginRedirect() {
    }

    /**
     * Stores the page to return to after login (a view path such as
     * {@code /pages/cars_details.xhtml?carId=...}).
     */
    public static void remember(String key, String viewPath) {
        FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put(key, viewPath);
    }

    /**
     * Returns the navigation outcome after a successful login: the remembered page
     * if there is one, otherwise {@code fallback}. The remembered page is cleared.
     */
    public static String consume(String key, String fallback) {
        Map<String, Object> session = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        Object target = session.remove(key);
        if (target == null) {
            return fallback;
        }
        String path = target.toString();
        return path + (path.contains("?") ? "&" : "?") + "faces-redirect=true";
    }
}
