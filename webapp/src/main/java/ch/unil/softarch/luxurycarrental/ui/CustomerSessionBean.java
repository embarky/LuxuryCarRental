package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.client.ApiTokens;
import ch.unil.softarch.luxurycarrental.domain.dto.CustomerSession;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/** The logged-in customer of this browser session. */
@Named("customerSessionBean")
@SessionScoped
public class CustomerSessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String email;
    private String password;
    private Customer customer;

    public String login() {
        try {
            CustomerSession session = api.loginCustomer(email, password);
            start(session);
            return LoginRedirect.consume(LoginRedirect.CUSTOMER_TARGET, "/pages/cars.xhtml?faces-redirect=true");
        } catch (ApiException e) {
            Messages.error(e.getMessage());
            return null;
        } finally {
            password = null;
        }
    }

    /** Starts the session after login or registration. */
    void start(CustomerSession session) {
        ApiTokens.store(ApiTokens.CUSTOMER, session.token);
        customer = session.customer;
        email = customer.getEmail();
    }

    public String logout() {
        customer = null;
        ApiTokens.clear(ApiTokens.CUSTOMER);
        Messages.info("You have been logged out.");
        Messages.keep();
        return "/index.xhtml?faces-redirect=true";
    }

    /** Reloads the customer (e.g. after a payment changed the balance). */
    public void refresh() {
        if (customer != null) {
            try {
                customer = api.customer(customer.getId());
            } catch (ApiException e) {
                if (e.isUnauthorized()) {
                    customer = null;
                    ApiTokens.clear(ApiTokens.CUSTOMER);
                }
            }
        }
    }

    public boolean isLoggedIn() {
        return customer != null;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Customer getCustomer() { return customer; }
    void setCustomer(Customer customer) { this.customer = customer; }
}
