package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.security.PasswordHasher;
import ch.unil.softarch.luxurycarrental.security.Role;
import ch.unil.softarch.luxurycarrental.service.VerificationCode.Purpose;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Issues and checks one-time email codes (password reset, email confirmation).
 * Codes are 6 digits, usable once, with at most 5 attempts; asking for a new code
 * invalidates the previous one.
 */
@ApplicationScoped
public class VerificationCodeService {

    public static final Duration PASSWORD_RESET_VALIDITY = Duration.ofMinutes(10);
    public static final Duration EMAIL_CONFIRMATION_VALIDITY = Duration.ofHours(24);

    private static final SecureRandom RANDOM = new SecureRandom();

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    public static Duration validity(Purpose purpose) {
        return purpose == Purpose.PASSWORD_RESET ? PASSWORD_RESET_VALIDITY : EMAIL_CONFIRMATION_VALIDITY;
    }

    /** Creates a new code (invalidating older ones) and returns it in clear text for the email. */
    @Transactional
    public String issue(Purpose purpose, Role role, String email) {
        for (VerificationCode old : open(purpose, role, email)) {
            old.markUsed();
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        em.persist(new VerificationCode(purpose, role, email, PasswordHasher.hash(code),
                LocalDateTime.now().plus(validity(purpose))));
        return code;
    }

    /** Checks a code and consumes it; throws a user-readable error if it is wrong or no longer valid. */
    @Transactional(dontRollbackOn = DomainException.class)
    public void consume(Purpose purpose, Role role, String email, String code) {
        LocalDateTime now = LocalDateTime.now();
        VerificationCode current = open(purpose, role, email).stream()
                .filter(c -> c.isUsable(now))
                .findFirst()
                .orElseThrow(() -> new DomainException("This code is no longer valid (expired, already used "
                        + "or too many attempts). Please ask for a new one."));
        if (!PasswordHasher.matches(code == null ? "" : code.trim(), current.getCodeHash())) {
            current.recordFailedAttempt();
            throw new DomainException("The verification code is not correct.");
        }
        current.markUsed();
    }

    private List<VerificationCode> open(Purpose purpose, Role role, String email) {
        return em.createQuery("SELECT c FROM VerificationCode c WHERE c.purpose = :purpose AND c.role = :role "
                        + "AND c.email = :email AND c.used = false ORDER BY c.expiresAt DESC", VerificationCode.class)
                .setParameter("purpose", purpose)
                .setParameter("role", role)
                .setParameter("email", email)
                .getResultList();
    }
}
