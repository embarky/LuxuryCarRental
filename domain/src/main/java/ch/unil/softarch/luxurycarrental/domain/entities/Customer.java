package ch.unil.softarch.luxurycarrental.domain.entities;

import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.model.Money;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

/**
 * A customer who rents cars.
 * <p>
 * The account balance is a simulated wallet: rentals and deposits are taken from it and
 * refunds go back to it. Money only moves through {@link #debit} and {@link #credit},
 * so the balance can never become negative.
 * </p>
 */
@Entity
@Table(name = "customer")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Minimum age to rent a car. */
    public static final int MINIMUM_AGE = 18;

    @Id
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    /** Password hash (never the password itself). Never sent in JSON. */
    @JsonbTransient
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String drivingLicenseNumber;

    @Column(nullable = false)
    private LocalDate drivingLicenseExpiryDate;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    /** Set when the customer enters the code sent to their email address. Required to pay. */
    private boolean emailVerified;

    /** Set by an administrator after checking the driving licence. Required to pay. */
    private boolean verifiedIdentity;

    private String billingAddress;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = Money.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate;

    public Customer() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
        balance = Money.of(balance);
    }

    // -------------------------------------------------------------------------
    // Business rules
    // -------------------------------------------------------------------------

    /** Age in full years today; computed from the date of birth so it never goes stale. */
    public int getAge() {
        return dateOfBirth == null ? 0 : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** True if the driving licence is still valid on the given day. */
    public boolean hasValidLicenceOn(LocalDate day) {
        return drivingLicenseExpiryDate != null && !drivingLicenseExpiryDate.isBefore(day);
    }

    /** True when both checks needed before a first payment are done: email confirmed and licence checked. */
    public boolean isReadyToPay() {
        return emailVerified && verifiedIdentity;
    }

    /** Takes money from the balance; refuses if the balance is too low. */
    public void debit(BigDecimal amount) {
        BigDecimal value = Money.requirePositive(amount, "The amount");
        if (balance.compareTo(value) < 0) {
            throw new DomainException("Insufficient balance: CHF " + value + " needed, CHF " + balance
                    + " available. Please top up your account.");
        }
        balance = Money.of(balance.subtract(value));
    }

    /** Adds money to the balance (top-up or refund). */
    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return;
        }
        balance = Money.of(balance.add(Money.requirePositive(amount, "The amount")));
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return firstName + " " + lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String passwordHash) { this.password = passwordHash; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDrivingLicenseNumber() { return drivingLicenseNumber; }
    public void setDrivingLicenseNumber(String drivingLicenseNumber) { this.drivingLicenseNumber = drivingLicenseNumber; }

    public LocalDate getDrivingLicenseExpiryDate() { return drivingLicenseExpiryDate; }
    public void setDrivingLicenseExpiryDate(LocalDate drivingLicenseExpiryDate) { this.drivingLicenseExpiryDate = drivingLicenseExpiryDate; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isVerifiedIdentity() { return verifiedIdentity; }
    public void setVerifiedIdentity(boolean verifiedIdentity) { this.verifiedIdentity = verifiedIdentity; }

    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

    public BigDecimal getBalance() { return balance; }
    /** For JSON and JPA only; business code uses {@link #debit} and {@link #credit}. */
    public void setBalance(BigDecimal balance) { this.balance = Money.of(balance); }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Customer other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + getFullName() + "', email='" + email + "'}";
    }
}
