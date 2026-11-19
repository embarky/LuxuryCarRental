package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.dto.BookedPeriod;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** A car's page: specifications, booked dates, live price and booking. */
@Named("carDetailsBean")
@ViewScoped
public class CarDetailsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    @Inject
    private CustomerSessionBean session;

    private String carId;
    private Car car;
    private List<BookedPeriod> booked = Collections.emptyList();
    private LocalDate start;
    private LocalDate end;

    /** Called from f:viewAction once the view parameters are set. */
    public void load() {
        if (car != null || FacesContext.getCurrentInstance().isPostback()) {
            return;
        }
        try {
            UUID id = UUID.fromString(carId);
            car = api.car(id);
            booked = api.bookedPeriods(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            Messages.error("This car does not exist.");
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    // ---- price preview ------------------------------------------------------

    public boolean isPeriodValid() {
        return start != null && end != null && end.isAfter(start) && !start.isBefore(LocalDate.now());
    }

    public long getDays() {
        return isPeriodValid() ? ChronoUnit.DAYS.between(start, end) : 0;
    }

    public BigDecimal getRental() {
        return car == null ? BigDecimal.ZERO : car.getDailyRentalPrice().multiply(BigDecimal.valueOf(getDays()));
    }

    public BigDecimal getTotalDue() {
        return car == null ? BigDecimal.ZERO : getRental().add(car.getDepositAmount());
    }

    /** True if the chosen dates overlap a booked period (shown before submitting). */
    public boolean isOverlapping() {
        if (!isPeriodValid()) {
            return false;
        }
        return booked.stream().anyMatch(p -> start.isBefore(p.endDate) && p.startDate.isBefore(end));
    }

    // ---- actions ------------------------------------------------------------

    /** Sends a visitor to the login page and back to this car afterwards. */
    public String loginToBook() {
        LoginRedirect.remember(LoginRedirect.CUSTOMER_TARGET, "/pages/cars_details.xhtml?carId=" + car.getId()
                + (isPeriodValid() ? "&start=" + start + "&end=" + end : ""));
        return "/pages/login.xhtml?faces-redirect=true";
    }

    public String book() {
        if (!session.isLoggedIn()) {
            return loginToBook();
        }
        if (!isPeriodValid()) {
            Messages.warn("Please choose a pick-up date from today and a later return date.");
            return null;
        }
        try {
            api.book(session.getCustomer().getId(), car.getId(), start, end);
            Messages.info("Your " + car.getDisplayName() + " is reserved. Pay within 24 hours to confirm it.");
            Messages.keep();
            return "/pages/orders.xhtml?faces-redirect=true";
        } catch (ApiException e) {
            Messages.error(e);
            booked = api.bookedPeriods(car.getId());
            return null;
        }
    }

    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }
    public Car getCar() { return car; }
    public List<BookedPeriod> getBooked() { return booked; }
    public LocalDate getStart() { return start; }
    public void setStart(LocalDate start) { this.start = start; }
    public LocalDate getEnd() { return end; }
    public void setEnd(LocalDate end) { this.end = end; }
}
