package ch.unil.softarch.luxurycarrental.domain;

/**
 * A business rule was violated, for example paying a booking that is already paid.
 * The message is written for the end user. The service layer turns it into HTTP 400
 * (or 409 for conflicts such as dates that are already booked).
 */
public class DomainException extends RuntimeException {

    private final boolean conflict;

    public DomainException(String message) {
        this(message, false);
    }

    private DomainException(String message, boolean conflict) {
        super(message);
        this.conflict = conflict;
    }

    /** A rule violated because of the current state of other data (e.g. dates already taken). */
    public static DomainException conflict(String message) {
        return new DomainException(message, true);
    }

    public boolean isConflict() {
        return conflict;
    }
}
