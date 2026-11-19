package ch.unil.softarch.luxurycarrental.client;

import jakarta.faces.context.FacesContext;

import java.util.Map;

/**
 * Keeps the API tokens of the logged-in customer and administrator in the HTTP session,
 * and picks the right one for the current page: admin pages use the admin token.
 */
public final class ApiTokens {

    public static final String CUSTOMER = "api.customerToken";
    public static final String ADMIN = "api.adminToken";

    private ApiTokens() {
    }

    public static void store(String key, String token) {
        session().put(key, token);
    }

    public static void clear(String key) {
        session().remove(key);
    }

    /** Token for an API call made while rendering or processing the current page, or null. */
    public static String current() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return null;
        }
        Map<String, Object> session = fc.getExternalContext().getSessionMap();
        String viewId = fc.getViewRoot() == null ? "" : fc.getViewRoot().getViewId();
        boolean adminPage = viewId != null && viewId.startsWith("/pages/admin/");
        Object first = session.get(adminPage ? ADMIN : CUSTOMER);
        Object second = session.get(adminPage ? CUSTOMER : ADMIN);
        return first != null ? first.toString() : second != null ? second.toString() : null;
    }

    private static Map<String, Object> session() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }
}
