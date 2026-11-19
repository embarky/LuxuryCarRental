package ch.unil.softarch.luxurycarrental.domain.model;

import ch.unil.softarch.luxurycarrental.domain.DomainException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A rental period from the pick-up date to the return date.
 * <p>
 * The car is picked up on {@code start} and returned on {@code end}, so a rental from
 * 1 to 4 June lasts 3 days. A rental lasts at least one day.
 * </p>
 */
public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        if (start == null || end == null) {
            throw new DomainException("Please choose a pick-up and a return date.");
        }
        if (!end.isAfter(start)) {
            throw new DomainException("The return date must be after the pick-up date.");
        }
    }

    /** Number of rental days (nights between pick-up and return). */
    public long days() {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Two periods overlap when they share at least one day of use. A car returned on
     * the 5th can be picked up again by someone else on the 5th.
     */
    public boolean overlaps(DateRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** True when the whole period is in the future or starts today. */
    public boolean startsOnOrAfter(LocalDate day) {
        return !start.isBefore(day);
    }
}
