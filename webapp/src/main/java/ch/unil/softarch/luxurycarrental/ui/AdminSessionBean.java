package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.client.ApiTokens;
import ch.unil.softarch.luxurycarrental.domain.dto.AdminSession;
import ch.unil.softarch.luxurycarrental.domain.entities.Admin;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/** The logged-in administrator of this browser session. */
@Named("adminSessionBean")
@SessionScoped
public class AdminSessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String email;
    private String password;
    private Admin admin;

    public String login() {
        try {
            AdminSession session = api.loginAdmin(email, password);
            ApiTokens.store(ApiTokens.ADMIN, session.token);
            admin = session.admin;
            return LoginRedirect.consume(LoginRedirect.ADMIN_TARGET, "/pages/admin/dashboard.xhtml?faces-redirect=true");
        } catch (ApiException e) {
            Messages.error(e.getMessage());
            return null;
        } finally {
            password = null;
        }
    }

    public String logout() {
        admin = null;
        ApiTokens.clear(ApiTokens.ADMIN);
        Messages.info("You have been logged out.");
        Messages.keep();
        return "/pages/staff-login.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return admin != null;
    }

    public String getInitial() {
        return admin == null || admin.getName() == null || admin.getName().isEmpty()
                ? "A" : admin.getName().substring(0, 1).toUpperCase();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Admin getAdmin() { return admin; }
}
