package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.CustomerClient;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.UUID;

@Named("customerSessionBean")
@SessionScoped
public class CustomerSessionBean implements Serializable {

    private String email;       // Customer email
    private String password;    // Customer password
    private UUID customerId;
    private Customer customer;  // Currently logged-in customer

    private final CustomerClient customerClient = new CustomerClient();

    /**
     * Customer login
     * @return Redirects to the customer page on success, returns null and shows an error message on failure
     */
    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();
        try {
            customer = customerClient.authenticate(email, password);

            if (customer != null) {
                // Login successful, redirect to customer page
                return "/index.xhtml?faces-redirect=true";
            } else {
                // Login failed
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Invalid email or password", null));
                return null;
            }
        } catch (Exception e) {
            // Catch any exception
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Login error: " + e.getMessage(), null));
            return null;
        }
    }

    /**
     * Customer logout
     * @return Redirects to the login page
     */
    public String logout() {
        customer = null;
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/index.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return customer != null;
    }

    // ===== Getters & Setters =====
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UUID getCustomerId() {return customerId; }

    public Customer getCustomer() { return customer; }
}