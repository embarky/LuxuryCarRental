package ch.unil.softarch.luxurycarrental.domain.enums;

/** What happened to the customer's money for a booking. */
public enum PaymentStatus {
    /** Nothing has been charged. */
    UNPAID,
    /** Rental and deposit have been charged. */
    PAID,
    /** Everything that was charged has been returned. */
    REFUNDED
}
