package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.api.Errors;
import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.dto.AdminRequest;
import ch.unil.softarch.luxurycarrental.domain.dto.PasswordChange;
import ch.unil.softarch.luxurycarrental.domain.entities.Admin;
import ch.unil.softarch.luxurycarrental.security.PasswordHasher;
import ch.unil.softarch.luxurycarrental.security.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Administrator accounts. Only administrators create other administrators. */
@ApplicationScoped
public class AdminService {

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    @Inject
    private PasswordResetService resetCodes;

    @Inject
    private Event<MailEvent> mail;

    @Transactional
    public Admin create(AdminRequest request) {
        if (request == null) {
            throw new DomainException("Administrator data is missing.");
        }
        String username = Validation.required(request.username, "Username");
        String email = Validation.email(request.email);
        String name = Validation.required(request.name, "Name");
        PasswordHasher.checkPolicy(request.password);

        if (count("a.username = :v", username) > 0) {
            throw DomainException.conflict("This username is already taken.");
        }
        if (count("LOWER(a.email) = :v", email) > 0) {
            throw DomainException.conflict("An administrator with this email already exists.");
        }
        Admin admin = new Admin(username, PasswordHasher.hash(request.password), name, email);
        em.persist(admin);
        mail.fire(MailEvent.of(admin.getEmail(), EmailTemplates.adminWelcome(admin)));
        return admin;
    }

    /** Creates the first administrator when there is none (called at start-up). */
    @Transactional
    public void ensureDefaultAdmin(String email, String password) {
        Long admins = em.createQuery("SELECT COUNT(a) FROM Admin a", Long.class).getSingleResult();
        if (admins == 0) {
            em.persist(new Admin("admin", PasswordHasher.hash(password), "Administrator", email.toLowerCase()));
            System.out.println("[AdminService] Created the default administrator " + email);
        }
    }

    public Admin authenticate(String email, String password) {
        return findByEmail(email)
                .filter(a -> PasswordHasher.matches(password, a.getPassword()))
                .orElseThrow(() -> Errors.unauthorized("Invalid email or password."));
    }

    public Admin get(UUID id) {
        Admin admin = em.find(Admin.class, id);
        if (admin == null) {
            throw Errors.notFound("Administrator not found.");
        }
        return admin;
    }

    public List<Admin> getAll() {
        return em.createQuery("SELECT a FROM Admin a ORDER BY a.createdAt", Admin.class).getResultList();
    }

    public Optional<Admin> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return em.createQuery("SELECT a FROM Admin a WHERE LOWER(a.email) = :email", Admin.class)
                .setParameter("email", email.trim().toLowerCase())
                .getResultStream()
                .findFirst();
    }

    /** Deletes another administrator; nobody can delete themselves or the last administrator. */
    @Transactional
    public void remove(UUID id, UUID callerId) {
        if (id.equals(callerId)) {
            throw new DomainException("You cannot delete your own account.");
        }
        Admin admin = get(id);
        Long admins = em.createQuery("SELECT COUNT(a) FROM Admin a", Long.class).getSingleResult();
        if (admins <= 1) {
            throw new DomainException("The last administrator cannot be deleted.");
        }
        em.remove(admin);
    }

    @Transactional
    public void changePassword(UUID id, PasswordChange change) {
        Admin admin = get(id);
        if (change == null || !PasswordHasher.matches(change.currentPassword, admin.getPassword())) {
            throw new DomainException("Your current password is not correct.");
        }
        PasswordHasher.checkPolicy(change.newPassword);
        admin.setPassword(PasswordHasher.hash(change.newPassword));
        mail.fire(MailEvent.of(admin.getEmail(), EmailTemplates.passwordChanged(admin.getName())));
    }

    @Transactional
    public void sendPasswordResetCode(String email) {
        findByEmail(email).ifPresent(a -> {
            String code = resetCodes.issue(Role.ADMIN, a.getEmail());
            mail.fire(MailEvent.of(a.getEmail(), EmailTemplates.passwordResetCode(a.getName(), code,
                    (int) PasswordResetService.VALIDITY.toMinutes())));
        });
    }

    @Transactional(dontRollbackOn = DomainException.class)
    public void resetPassword(String email, String code, String newPassword) {
        Admin admin = findByEmail(email)
                .orElseThrow(() -> new DomainException("This code has expired. Please ask for a new one."));
        PasswordHasher.checkPolicy(newPassword);
        resetCodes.consume(Role.ADMIN, admin.getEmail(), code);
        admin.setPassword(PasswordHasher.hash(newPassword));
        mail.fire(MailEvent.of(admin.getEmail(), EmailTemplates.passwordChanged(admin.getName())));
    }

    private long count(String condition, String value) {
        return em.createQuery("SELECT COUNT(a) FROM Admin a WHERE " + condition, Long.class)
                .setParameter("v", value)
                .getSingleResult();
    }
}
