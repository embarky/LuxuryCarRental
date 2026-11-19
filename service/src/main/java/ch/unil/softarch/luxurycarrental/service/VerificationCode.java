package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.security.Role;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A one-time code sent by email, to reset a password or to confirm an email address.
 * Stored hashed, so that it survives a server restart and cannot be read from the table.
 */
@Entity
@Table(name = "verification_code")
public class VerificationCode {

    /** What a code may be used for; a code for one purpose is never accepted for another. */
    public enum Purpose { PASSWORD_RESET, EMAIL_CONFIRMATION }

    static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(length = 36)
    private UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private int attempts;

    private boolean used;

    protected VerificationCode() {
    }

    VerificationCode(Purpose purpose, Role role, String email, String codeHash, LocalDateTime expiresAt) {
        this.purpose = purpose;
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
