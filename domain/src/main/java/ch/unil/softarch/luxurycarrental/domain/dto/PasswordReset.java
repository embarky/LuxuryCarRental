package ch.unil.softarch.luxurycarrental.domain.dto;

/** Request a code (email only) or set a new password with it (all fields). */
public class PasswordReset {
    public String email;
    public String code;
    public String newPassword;
}
