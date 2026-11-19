package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Admin list of customers, and one customer's page ({@code ?id=}). */
@Named("customerAdminBean")
@ViewScoped
public class CustomerAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String text;
    private boolean onlyUnverified;
    private List<Customer> customers = Collections.emptyList();

    private String id;
    private Customer customer;
    private List<Booking> bookings = Collections.emptyList();

    @PostConstruct
    public void init() {
        String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id");
        if (param == null) {
            load();
        }
    }

    public void load() {
        try {
            List<Customer> all = api.customers();
            if (onlyUnverified) {
                all = all.stream().filter(c -> !c.isVerifiedIdentity()).toList();
            }
            if (text != null && !text.isBlank()) {
                String q = text.trim().toLowerCase(Locale.ROOT);
                all = all.stream().filter(c -> c.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || c.getEmail().toLowerCase(Locale.ROOT).contains(q)
                        || (c.getDrivingLicenseNumber() != null && c.getDrivingLicenseNumber().toLowerCase(Locale.ROOT).contains(q))).toList();
            }
            customers = all;
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    /** f:viewAction of the customer page. */
    public void loadOne() {
        if (FacesContext.getCurrentInstance().isPostback() || id == null) {
            return;
        }
        reloadOne();
    }

    private void reloadOne() {
        try {
            UUID uuid = UUID.fromString(id);
            customer = api.customer(uuid);
            bookings = api.bookingsOfCustomer(uuid);
        } catch (ApiException e) {
            Messages.error(e);
        } catch (IllegalArgumentException e) {
            Messages.error("This customer does not exist.");
        }
    }

    /** Support override, e.g. when the customer could not receive the email. */
    public void confirmEmail(Customer c) {
        try {
            api.setEmailVerified(c.getId(), true);
            Messages.info("The email address of " + c.getFullName() + " is now marked as confirmed.");
        } catch (ApiException e) {
            Messages.error(e);
        }
        reloadOne();
    }

    public void setVerified(Customer c, boolean verified) {
        try {
            api.setVerified(c.getId(), verified);
            Messages.info(c.getFullName() + (!verified ? " is no longer verified."
                    : c.isEmailVerified() ? " is now verified and can pay bookings."
                    : " is now verified; they still need to confirm their email before paying."));
        } catch (ApiException e) {
            Messages.error(e);
        }
        if (customer != null) {
            reloadOne();
        } else {
            load();
        }
    }

    public String delete(Customer c) {
        try {
            api.deleteCustomer(c.getId());
            Messages.info(c.getFullName() + " has been deleted.");
            Messages.keep();
            return "/pages/admin/customers.xhtml?faces-redirect=true";
        } catch (ApiException e) {
            Messages.error(e);
            return null;
        }
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isOnlyUnverified() { return onlyUnverified; }
    public void setOnlyUnverified(boolean onlyUnverified) { this.onlyUnverified = onlyUnverified; }
    public List<Customer> getCustomers() { return customers; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public List<Booking> getBookings() { return bookings; }
}
