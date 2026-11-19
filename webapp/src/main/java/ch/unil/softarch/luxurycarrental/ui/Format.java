package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting used in the pages: {@code #{fmt.chf(car.dailyRentalPrice)}}, {@code #{fmt.date(b.startDate)}}. */
@Named("fmt")
@ApplicationScoped
public class Format {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public String chf(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return String.format(Locale.ROOT, "CHF %,.2f", value).replace(',', '\'');
    }

    public String date(LocalDate date) {
        return date == null ? "–" : DATE.format(date);
    }

    public String dateTime(LocalDateTime time) {
        return time == null ? "–" : DATE_TIME.format(time);
    }

    /** yyyy-MM-dd, e.g. for the min attribute of a date input. */
    public String iso(LocalDate date) {
        return date == null ? "" : ISO.format(date);
    }

    public LocalDate getTodayDate() {
        return LocalDate.now();
    }

    public String today() {
        return ISO.format(LocalDate.now());
    }

    public String tomorrow() {
        return ISO.format(LocalDate.now().plusDays(1));
    }

    public String status(BookingStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Awaiting payment";
            case CONFIRMED -> "Confirmed";
            case ACTIVE -> "In progress";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
            case REJECTED -> "Rejected";
            case EXPIRED -> "Expired";
        };
    }

    /** CSS class of the status badge. */
    public String statusClass(BookingStatus status) {
        return status == null ? "badge" : "badge badge-" + status.name().toLowerCase(Locale.ROOT);
    }

    public String carStatus(CarStatus status) {
        return status == CarStatus.MAINTENANCE ? "In maintenance" : "In service";
    }

    public String carStatusClass(CarStatus status) {
        return status == CarStatus.MAINTENANCE ? "badge badge-rejected" : "badge badge-completed";
    }
}
