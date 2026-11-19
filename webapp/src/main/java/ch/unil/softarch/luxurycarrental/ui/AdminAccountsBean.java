package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.dto.AdminRequest;
import ch.unil.softarch.luxurycarrental.domain.dto.PasswordChange;
import ch.unil.softarch.luxurycarrental.domain.entities.Admin;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/** Administrator accounts: list, create, delete, and the own password. */
@Named("adminAccountsBean")
@ViewScoped
public class AdminAccountsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    @Inject
    private AdminSessionBean session;

    private List<Admin> admins = Collections.emptyList();
    private AdminRequest form = new AdminRequest();
    private final PasswordChange password = new PasswordChange();
    private String confirmPassword;

    @PostConstruct
    public void init() {
        load();
    }

    private void load() {
        try {
            admins = api.admins();
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    public void create() {
        try {
            Admin admin = api.createAdmin(form);
            Messages.info("Administrator " + admin.getUsername() + " created.");
            form = new AdminRequest();
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public void delete(Admin admin) {
        try {
            api.deleteAdmin(admin.getId());
            Messages.info("Administrator " + admin.getUsername() + " deleted.");
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public void changePassword() {
        if (password.newPassword == null || !password.newPassword.equals(confirmPassword)) {
            Messages.error("The two new passwords do not match.");
            return;
        }
        try {
            Messages.info(api.changeOwnAdminPassword(password));
        } catch (ApiException e) {
            Messages.error(e);
        } finally {
            password.currentPassword = null;
            password.newPassword = null;
            confirmPassword = null;
        }
    }

    public boolean isMe(Admin admin) {
        return session.getAdmin() != null && session.getAdmin().getId().equals(admin.getId());
    }

    public List<Admin> getAdmins() { return admins; }
    public AdminRequest getForm() { return form; }
    public PasswordChange getPassword() { return password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
