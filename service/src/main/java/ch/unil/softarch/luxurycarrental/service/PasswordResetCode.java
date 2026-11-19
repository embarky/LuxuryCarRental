package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.security.Role;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A one-time code to reset a password, stored hashed in the database so that it survives
 * a server restart and cannot be read from the table.
 */
@Entity
@Table(name = "password_reset_code")
public class PasswordResetCode {

    static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(length = 36)
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private int attempts;

    private boolean used;

    protected PasswordResetCode() {
    }

    PasswordResetCode(Role role, String email, String codeHash, LocalDateTime expiresAt) {
        this.role = role;
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    boolean isUsable(LocalDateTime now) {
        return !used && attempts < MAX_ATTEMPTS && now.isBefore(expiresAt);
    }

    void recordFailedAttempt() {
        attempts++;
    }

    void markUsed() {
        used = true;
    }

    String getCodeHash() {
        return codeHash;
    }
}
