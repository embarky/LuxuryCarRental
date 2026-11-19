package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.CarClient;
import ch.unil.softarch.luxurycarrental.client.BookingClient;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.ui.CustomerSessionBean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Named("carDetailsBean")
@ViewScoped
public class CarDetailsBean implements Serializable {

    @Inject
    private CarClient carClient;

    @Inject
    private BookingClient bookingClient;

    @Inject
    private CustomerSessionBean customerSessionBean;

    private Car car;
    private LocalDate startDate;
    private LocalDate endDate;

    @PostConstruct
    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
        String idStr = params.get("carId");
        if (idStr != null) {
            try {
                car = carClient.getCar(UUID.fromString(idStr));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                                "Car not found: " + e.getMessage()));
            }
        }
    }

    // Getter / Setter
    public Car getCar() { return car; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    /**
     * Sends a visitor who is not logged in to the login page, and back to this car afterwards.
     */
    public String loginToBook() {
        String target = "/pages/cars_details.xhtml" + (car != null ? "?carId=" + car.getId() : "");
        LoginRedirect.remember(LoginRedirect.CUSTOMER_TARGET, target);
        return "/pages/login.xhtml?faces-redirect=true";
    }

    // Book the car
    public String bookCar() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (!customerSessionBean.isLoggedIn() || customerSessionBean.getCustomer() == null) {
            // The session may have expired while the page was open
            return loginToBook();
        }

        if (startDate == null || endDate == null) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Error", "Please select start and end dates."));
            return null;
        }

        if (!endDate.isAfter(startDate)) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Error", "End date must be after start date."));
            return null;
        }

        try {
            UUID customerId = customerSessionBean.getCustomer().getId(); // logged-in customer from the session
            bookingClient.createBooking(customerId, car.getId(), startDate, endDate);

            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                            "Booking created successfully for car "
                                    + car.getCarType().getBrand() + " " + car.getCarType().getModel()
                                    + " from " + startDate + " to " + endDate));

            startDate = null;
            endDate = null;

        } catch (Exception e) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "Failed to create booking: " + e.getMessage()));
        }
        return null;
    }
}