package ch.unil.softarch.luxurycarrental.domain.enums;

/**
 * Life cycle of a booking.
 * <pre>
 * PENDING ──pay──▶ CONFIRMED ──pick up──▶ ACTIVE ──return──▶ COMPLETED
 *    │ └─timeout──▶ EXPIRED      │
 *    ├─cancel / reject ◀─────────┘ (full refund once paid)
 * </pre>
 */
public enum BookingStatus {
    /** Created by the customer, not paid yet. Holds the dates until it expires. */
    PENDING,
    /** Paid (rental + deposit). Waiting for pick-up. */
    CONFIRMED,
    /** The customer has the car. */
    ACTIVE,
    /** Car returned, deposit refunded. */
    COMPLETED,
    /** Cancelled by the customer. */
    CANCELLED,
    /** Refused by an administrator. */
    REJECTED,
    /** Not paid within the payment window. */
    EXPIRED;

    /** Bookings in these states occupy the car for their dates. */
    public boolean blocksCar() {
        return this == PENDING || this == CONFIRMED || this == ACTIVE;
    }

    /** No further transition is possible. */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == REJECTED || this == EXPIRED;
    }
}
