package ch.unil.softarch.luxurycarrental.domain.dto;

/** Request a code (email only) or set a new password with it (all fields). */
public class PasswordReset {
    public String email;
    public String code;
    public String newPassword;

    // Getters and setters for JSF forms (JSON-B uses the public fields)
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
