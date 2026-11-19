package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.ApiException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

/** Short helpers for page messages; {@link #keep()} carries them across a redirect. */
public final class Messages {

    private Messages() {
    }

    public static void info(String text) {
        add(FacesMessage.SEVERITY_INFO, text);
    }

    public static void warn(String text) {
        add(FacesMessage.SEVERITY_WARN, text);
    }

    public static void error(String text) {
        add(FacesMessage.SEVERITY_ERROR, text);
    }

    /** Shows the API's explanation of a failed call. */
    public static void error(ApiException e) {
        error(e.isUnauthorized() ? "Your session has expired. Please log in again." : e.getMessage());
    }

    /** Keeps the messages of this request for the page shown after a redirect. */
    public static void keep() {
        FacesContext.getCurrentInstance().getExternalContext().getFlash().setKeepMessages(true);
    }

    private static void add(FacesMessage.Severity severity, String text) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, text, null));
    }
}
