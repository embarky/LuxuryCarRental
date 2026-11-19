package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.dto.RegistrationRequest;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/** Customer sign-up. The account is logged in straight away. */
@Named("registerBean")
@ViewScoped
public class RegisterBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    @Inject
    private CustomerSessionBean session;

    private final RegistrationRequest form = new RegistrationRequest();
    private String confirmPassword;

    public String register() {
        if (form.password == null || !form.password.equals(confirmPassword)) {
            Messages.error("The two passwords do not match.");
            return null;
        }
        try {
            api.register(form);
            session.start(api.loginCustomer(form.email, form.password));
            Messages.info("Welcome, " + form.firstName + "! We have sent a code to " + form.email
                    + " to confirm your email address.");
            Messages.keep();
            return LoginRedirect.consume(LoginRedirect.CUSTOMER_TARGET, "/pages/account.xhtml?faces-redirect=true");
        } catch (ApiException e) {
            Messages.error(e);
            return null;
        } finally {
            form.password = null;
            confirmPassword = null;
        }
    }

    public RegistrationRequest getForm() { return form; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
