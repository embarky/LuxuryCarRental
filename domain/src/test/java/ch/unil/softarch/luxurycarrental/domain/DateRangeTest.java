package ch.unil.softarch.luxurycarrental.domain;

import ch.unil.softarch.luxurycarrental.domain.model.DateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    private static DateRange range(int startDay, int endDay) {
        return new DateRange(LocalDate.of(2026, 6, startDay), LocalDate.of(2026, 6, endDay));
    }

    @Test
    void countsNightsBetweenPickUpAndReturn() {
        assertEquals(3, range(1, 4).days());
    }

    @Test
    void returnMustBeAfterPickUp() {
        assertThrows(DomainException.class, () -> range(4, 4));
        assertThrows(DomainException.class, () -> range(5, 4));
        assertThrows(DomainException.class, () -> new DateRange(null, LocalDate.of(2026, 6, 4)));
    }

    @Test
    void overlappingPeriods() {
        assertTrue(range(1, 5).overlaps(range(3, 8)));
        assertTrue(range(3, 8).overlaps(range(1, 5)));
        assertTrue(range(1, 10).overlaps(range(3, 4)));
    }

    @Test
    void backToBackPeriodsDoNotOverlap() {
        // returned on the 5th, picked up again by someone else on the 5th
        assertFalse(range(1, 5).overlaps(range(5, 8)));
        assertFalse(range(5, 8).overlaps(range(1, 5)));
    }
}
