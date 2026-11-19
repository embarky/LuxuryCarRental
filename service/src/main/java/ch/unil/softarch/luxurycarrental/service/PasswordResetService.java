package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.security.PasswordHasher;
import ch.unil.softarch.luxurycarrental.security.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Issues and checks password-reset codes for customers and administrators.
 * Codes are 6 digits, valid for {@link #VALIDITY}, usable once, with at most 5 attempts.
 */
@ApplicationScoped
public class PasswordResetService {

    public static final Duration VALIDITY = Duration.ofMinutes(10);

    private static final SecureRandom RANDOM = new SecureRandom();

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    /** Creates a new code (invalidating older ones) and returns it in clear text for the email. */
    @Transactional
    public String issue(Role role, String email) {
        for (PasswordResetCode old : open(role, email)) {
            old.markUsed();
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        em.persist(new PasswordResetCode(role, email, PasswordHasher.hash(code),
                LocalDateTime.now().plus(VALIDITY)));
        return code;
    }

    /** Checks a code and consumes it; throws a user-readable error if it is wrong or expired. */
    @Transactional(dontRollbackOn = DomainException.class)
    public void consume(Role role, String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode current = open(role, email).stream()
                .filter(c -> c.isUsable(now))
                .findFirst()
                .orElseThrow(() -> new DomainException("This code has expired. Please ask for a new one."));
        if (!PasswordHasher.matches(code == null ? "" : code.trim(), current.getCodeHash())) {
            current.recordFailedAttempt();
            throw new DomainException("The verification code is not correct.");
        }
        current.markUsed();
    }

    private List<PasswordResetCode> open(Role role, String email) {
        return em.createQuery("SELECT c FROM PasswordResetCode c WHERE c.role = :role AND c.email = :email "
                        + "AND c.used = false ORDER BY c.expiresAt DESC", PasswordResetCode.class)
                .setParameter("role", role)
                .setParameter("email", email)
                .getResultList();
    }
}
