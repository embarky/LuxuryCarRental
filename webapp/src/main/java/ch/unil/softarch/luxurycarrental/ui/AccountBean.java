package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.dto.PasswordChange;
import ch.unil.softarch.luxurycarrental.domain.dto.ProfileUpdate;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;

/** "My account": profile and licence, balance top-up, password. */
@Named("accountBean")
@ViewScoped
public class AccountBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    @Inject
    private CustomerSessionBean session;

    private final ProfileUpdate profile = new ProfileUpdate();
    private final PasswordChange password = new PasswordChange();
    private String confirmPassword;
    private BigDecimal topUpAmount;

    @PostConstruct
    public void init() {
        session.refresh();
        Customer c = session.getCustomer();
        if (c != null) {
            profile.firstName = c.getFirstName();
            profile.lastName = c.getLastName();
            profile.phoneNumber = c.getPhoneNumber();
            profile.billingAddress = c.getBillingAddress();
            profile.drivingLicenseNumber = c.getDrivingLicenseNumber();
            profile.drivingLicenseExpiryDate = c.getDrivingLicenseExpiryDate();
        }
    }

    public void saveProfile() {
        try {
            session.setCustomer(api.updateProfile(getCustomer().getId(), profile));
            Messages.info("Your details have been saved.");
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    public void topUp() {
        try {
            session.setCustomer(api.topUp(getCustomer().getId(), topUpAmount));
            Messages.info("Your balance is now " + new Format().chf(getCustomer().getBalance()) + ".");
            topUpAmount = null;
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    public void quickTopUp(int amount) {
        topUpAmount = BigDecimal.valueOf(amount);
        topUp();
    }

    public void changePassword() {
        if (password.newPassword == null || !password.newPassword.equals(confirmPassword)) {
            Messages.error("The two new passwords do not match.");
            return;
        }
        try {
            Messages.info(api.changeCustomerPassword(getCustomer().getId(), password));
        } catch (ApiException e) {
            Messages.error(e);
        } finally {
            password.currentPassword = null;
            password.newPassword = null;
            confirmPassword = null;
        }
    }

    public Customer getCustomer() { return session.getCustomer(); }
    public ProfileUpdate getProfile() { return profile; }
    public PasswordChange getPassword() { return password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public BigDecimal getTopUpAmount() { return topUpAmount; }
    public void setTopUpAmount(BigDecimal topUpAmount) { this.topUpAmount = topUpAmount; }
}
