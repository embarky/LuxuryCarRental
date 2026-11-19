package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * Password reset in two steps: ask for a code by email, then set a new password with it.
 * {@code ?role=admin} resets an administrator account.
 */
@Named("resetBean")
@ViewScoped
public class ResetBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String role;
    private String email;
    private String code;
    private String newPassword;
    private String confirmPassword;
    private boolean codeSent;

    public void sendCode() {
        try {
            Messages.info(api.sendResetCode(isAdmin(), email));
            codeSent = true;
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    public String reset() {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            Messages.error("The two passwords do not match.");
            return null;
        }
        try {
            Messages.info(api.resetPassword(isAdmin(), email, code, newPassword));
            Messages.keep();
            return (isAdmin() ? "/pages/staff-login.xhtml" : "/pages/login.xhtml") + "?faces-redirect=true";
        } catch (ApiException e) {
            Messages.error(e);
            return null;
        } finally {
            newPassword = null;
            confirmPassword = null;
        }
    }

    public void changeEmail() {
        codeSent = false;
        code = null;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public boolean isCodeSent() { return codeSent; }
}
