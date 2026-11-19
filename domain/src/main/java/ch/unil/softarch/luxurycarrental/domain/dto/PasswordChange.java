package ch.unil.softarch.luxurycarrental.domain.dto;

public class PasswordChange {
    public String currentPassword;
    public String newPassword;

    // Getters and setters for JSF forms (JSON-B uses the public fields)
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
