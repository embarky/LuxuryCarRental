package ch.unil.softarch.luxurycarrental.domain.enums;

/**
 * Whether a car can be rented at all. Availability for specific dates is computed
 * from the bookings, not stored here.
 */
public enum CarStatus {
    /** In service: can be booked for any free dates. */
    AVAILABLE,
    /** Out of service (repair, cleaning, ...): cannot be booked. */
    MAINTENANCE
}
