package ch.unil.softarch.luxurycarrental.domain.dto;

import java.time.LocalDate;

/** Dates during which a car is already taken (no customer data). */
public class BookedPeriod {
    public LocalDate startDate;
    public LocalDate endDate;

    public BookedPeriod() {
    }

    public BookedPeriod(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
