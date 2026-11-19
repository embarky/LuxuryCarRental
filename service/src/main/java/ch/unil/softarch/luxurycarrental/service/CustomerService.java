package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.api.Errors;
import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.dto.PasswordChange;
import ch.unil.softarch.luxurycarrental.domain.dto.ProfileUpdate;
import ch.unil.softarch.luxurycarrental.domain.dto.RegistrationRequest;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import ch.unil.softarch.luxurycarrental.domain.model.Money;
import ch.unil.softarch.luxurycarrental.security.PasswordHasher;
import ch.unil.softarch.luxurycarrental.security.Role;
import ch.unil.softarch.luxurycarrental.service.VerificationCode.Purpose;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Customer accounts: registration, profile, balance top-ups, identity verification and passwords.
 */
@ApplicationScoped
public class CustomerService {

    /** Largest single top-up of the simulated wallet. */
    private static final BigDecimal MAX_TOP_UP = new BigDecimal("100000");

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    @Inject
    private VerificationCodeService codes;

    @Inject
    private Event<MailEvent> mail;

    // -------------------------------------------------------------------------
    // Registration and login
    // -------------------------------------------------------------------------

    @Transactional
    public Customer register(RegistrationRequest request) {
        if (request == null) {
            throw new DomainException("Registration data is missing.");
        }
        Customer c = new Customer();
        c.setFirstName(Validation.required(request.firstName, "First name"));
        c.setLastName(Validation.required(request.lastName, "Last name"));
        c.setEmail(Validation.email(request.email));
        PasswordHasher.checkPolicy(request.password);
        c.setPassword(PasswordHasher.hash(request.password));
        c.setPhoneNumber(Validation.phone(request.phoneNumber));
        c.setDateOfBirth(Validation.required(request.dateOfBirth, "Date of birth"));
        c.setDrivingLicenseNumber(Validation.required(request.drivingLicenseNumber, "Driving licence number"));
        c.setDrivingLicenseExpiryDate(Validation.required(request.drivingLicenseExpiryDate, "Driving licence expiry date"));
        c.setBillingAddress(Validation.optional(request.billingAddress));
        c.setVerifiedIdentity(false);
        c.setBalance(Money.ZERO);

        if (c.getAge() < Customer.MINIMUM_AGE) {
            throw new DomainException("You must be at least " + Customer.MINIMUM_AGE + " years old to rent a car.");
        }
        if (!c.getDrivingLicenseExpiryDate().isAfter(LocalDate.now())) {
            throw new DomainException("Your driving licence has expired.");
        }
        checkUnique(null, c.getEmail(), c.getPhoneNumber(), c.getDrivingLicenseNumber());

        c.setEmailVerified(false);

        em.persist(c);
        String code = codes.issue(Purpose.EMAIL_CONFIRMATION, Role.CUSTOMER, c.getEmail());
        mail.fire(MailEvent.of(c.getEmail(), EmailTemplates.customerWelcome(c, code,
                (int) VerificationCodeService.EMAIL_CONFIRMATION_VALIDITY.toHours())));
        return c;
    }

    /** The customer with these credentials, or 401. The same message for both cases avoids user enumeration. */
    public Customer authenticate(String email, String password) {
        return findByEmail(email)
                .filter(c -> PasswordHasher.matches(password, c.getPassword()))
                .orElseThrow(() -> Errors.unauthorized("Invalid email or password."));
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Customer get(UUID id) {
        Customer c = em.find(Customer.class, id);
        if (c == null) {
            throw Errors.notFound("Customer not found.");
        }
        return c;
    }

    public List<Customer> getAll() {
        return em.createQuery("SELECT c FROM Customer c ORDER BY c.creationDate DESC", Customer.class).getResultList();
    }

    public Optional<Customer> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return em.createQuery("SELECT c FROM Customer c WHERE LOWER(c.email) = :email", Customer.class)
                .setParameter("email", email.trim().toLowerCase())
                .getResultStream()
                .findFirst();
    }

    // -------------------------------------------------------------------------
    // Changes
    // -------------------------------------------------------------------------

    @Transactional
    public Customer updateProfile(UUID id, ProfileUpdate update) {
        Customer c = get(id);
        if (update == null) {
            return c;
        }
        if (update.firstName != null) c.setFirstName(Validation.required(update.firstName, "First name"));
        if (update.lastName != null) c.setLastName(Validation.required(update.lastName, "Last name"));
        if (update.billingAddress != null) c.setBillingAddress(Validation.optional(update.billingAddress));
        if (update.phoneNumber != null) {
            String phone = Validation.phone(update.phoneNumber);
            checkUnique(id, null, phone, null);
            c.setPhoneNumber(phone);
        }
        if (update.drivingLicenseNumber != null) {
            String licence = Validation.required(update.drivingLicenseNumber, "Driving licence number");
            if (!licence.equals(c.getDrivingLicenseNumber())) {
                checkUnique(id, null, null, licence);
                c.setDrivingLicenseNumber(licence);
                c.setVerifiedIdentity(false);   // a new licence has to be checked again
            }
        }
        if (update.drivingLicenseExpiryDate != null) {
            if (!update.drivingLicenseExpiryDate.isAfter(LocalDate.now())) {
                throw new DomainException("The new driving licence expiry date must be in the future.");
            }
            c.setDrivingLicenseExpiryDate(update.drivingLicenseExpiryDate);
        }
        return c;
    }

    /** Adds money to the simulated wallet (there is no real payment provider). */
    @Transactional
    public Customer topUp(UUID id, BigDecimal amount) {
        BigDecimal value = Money.requirePositive(amount, "The top-up amount");
        if (value.compareTo(MAX_TOP_UP) > 0) {
            throw new DomainException("A single top-up is limited to CHF " + MAX_TOP_UP + ".");
        }
        Customer c = get(id);
        c.credit(value);
        return c;
    }

    @Transactional
    public Customer setVerified(UUID id, boolean verified) {
        Customer c = get(id);
        boolean newlyVerified = verified && !c.isVerifiedIdentity();
        c.setVerifiedIdentity(verified);
        if (newlyVerified) {
            mail.fire(MailEvent.of(c.getEmail(), EmailTemplates.identityVerified(c)));
        }
        return c;
    }

    @Transactional
    public void changePassword(UUID id, PasswordChange change) {
        Customer c = get(id);
        if (change == null || !PasswordHasher.matches(change.currentPassword, c.getPassword())) {
            throw new DomainException("Your current password is not correct.");
        }
        PasswordHasher.checkPolicy(change.newPassword);
        c.setPassword(PasswordHasher.hash(change.newPassword));
        mail.fire(MailEvent.of(c.getEmail(), EmailTemplates.passwordChanged(c.getFirstName())));
    }

    /**
     * Deletes an account without any booking. Accounts with a booking history are kept,
     * because the bookings must stay traceable.
     */
    @Transactional
    public void remove(UUID id) {
        Customer c = get(id);
        Long bookings = em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.customer.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        if (bookings > 0) {
            throw DomainException.conflict("This customer has " + bookings + " booking(s) in their history "
                    + "and cannot be deleted.");
        }
        em.remove(c);
    }

    // -------------------------------------------------------------------------
    // Email confirmation
    // -------------------------------------------------------------------------

    /** Sends a new confirmation code (the previous one stops working). */
    @Transactional
    public void sendEmailConfirmationCode(UUID id) {
        Customer c = get(id);
        if (c.isEmailVerified()) {
            throw new DomainException("Your email address is already confirmed.");
        }
        String code = codes.issue(Purpose.EMAIL_CONFIRMATION, Role.CUSTOMER, c.getEmail());
        mail.fire(MailEvent.of(c.getEmail(), EmailTemplates.emailConfirmationCode(c.getFirstName(), code,
                (int) VerificationCodeService.EMAIL_CONFIRMATION_VALIDITY.toHours())));
    }

    @Transactional(dontRollbackOn = DomainException.class)
    public Customer confirmEmail(UUID id, String code) {
        Customer c = get(id);
        if (!c.isEmailVerified()) {
            codes.consume(Purpose.EMAIL_CONFIRMATION, Role.CUSTOMER, c.getEmail(), code);
            c.setEmailVerified(true);
        }
        return c;
    }

    /** Support override by an administrator, e.g. when the customer confirmed by phone. */
    @Transactional
    public Customer setEmailVerified(UUID id, boolean verified) {
        Customer c = get(id);
        c.setEmailVerified(verified);
        return c;
    }

    // -------------------------------------------------------------------------
    // Password reset
    // -------------------------------------------------------------------------

    /** Sends a code by email. Unknown addresses get the same answer, to not reveal who has an account. */
    @Transactional
    public void sendPasswordResetCode(String email) {
        findByEmail(email).ifPresent(c -> {
            String code = codes.issue(Purpose.PASSWORD_RESET, Role.CUSTOMER, c.getEmail());
            mail.fire(MailEvent.of(c.getEmail(), EmailTemplates.passwordResetCode(c.getFirstName(), code,
                    (int) VerificationCodeService.PASSWORD_RESET_VALIDITY.toMinutes())));
        });
    }

    @Transactional(dontRollbackOn = DomainException.class)
    public void resetPassword(String email, String code, String newPassword) {
        Customer c = findByEmail(email)
                .orElseThrow(() -> new DomainException("This code is no longer valid. Please ask for a new one."));
        PasswordHasher.checkPolicy(newPassword);
        codes.consume(Purpose.PASSWORD_RESET, Role.CUSTOMER, c.getEmail(), code);
        c.setPassword(PasswordHasher.hash(newPassword));
        mail.fire(MailEvent.of(c.getEmail(), EmailTemplates.passwordChanged(c.getFirstName())));
    }

    // -------------------------------------------------------------------------

    private void checkUnique(UUID self, String email, String phone, String licence) {
        if (email != null && taken("LOWER(c.email)", email.toLowerCase(), self)) {
            throw DomainException.conflict("An account with this email already exists.");
        }
        if (phone != null && taken("c.phoneNumber", phone, self)) {
            throw DomainException.conflict("This phone number is already used by another account.");
        }
        if (licence != null && taken("c.drivingLicenseNumber", licence, self)) {
            throw DomainException.conflict("This driving licence number is already registered.");
        }
    }

    private boolean taken(String field, String value, UUID self) {
        var query = em.createQuery("SELECT COUNT(c) FROM Customer c WHERE " + field + " = :v"
                        + (self == null ? "" : " AND c.id <> :self"), Long.class)
                .setParameter("v", value);
        if (self != null) {
            query.setParameter("self", self);
        }
        return query.getSingleResult() > 0;
    }
}
