package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** The car catalogue with its filters (home page and Cars page). */
@Named("carsBean")
@ViewScoped
public class CarsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String text;
    private String category;
    private BigDecimal maxPrice;
    private LocalDate start;
    private LocalDate end;

    private List<Car> cars = Collections.emptyList();
    private List<String> categories = Collections.emptyList();

    @PostConstruct
    public void init() {
        try {
            categories = api.categories();
        } catch (ApiException e) {
            Messages.error(e);
        }
        search();
    }

    public void search() {
        if ((start == null) != (end == null)) {
            Messages.warn("Choose both a pick-up and a return date to see the cars free on those dates.");
            return;
        }
        if (start != null && !end.isAfter(start)) {
            Messages.warn("The return date must be after the pick-up date.");
            return;
        }
        if (start != null && start.isBefore(LocalDate.now())) {
            Messages.warn("The pick-up date cannot be in the past.");
            return;
        }
        try {
            cars = api.searchCars(text, category, maxPrice, start, end);
        } catch (ApiException e) {
            cars = Collections.emptyList();
            Messages.error(e);
        }
    }

    /** f:viewAction of the Cars page: dates may arrive in the URL (from the home page). */
    public void searchIfDates() {
        if (start != null || end != null) {
            search();
        }
    }

    /** Home page search: opens the Cars page with the chosen dates. */
    public String browse() {
        if ((start == null) != (end == null) || (start != null && !end.isAfter(start))) {
            Messages.warn("Choose a pick-up date and a later return date, or leave both empty.");
            return null;
        }
        return "/pages/cars.xhtml?faces-redirect=true" + (isDatesChosen() ? "&start=" + start + "&end=" + end : "");
    }

    public void reset() {
        text = null;
        category = null;
        maxPrice = null;
        start = null;
        end = null;
        search();
    }

    public boolean isDatesChosen() {
        return start != null && end != null;
    }

    /** Link to a car page, carrying the chosen dates. */
    public String detailsQuery(Car car) {
        return "carId=" + car.getId() + (isDatesChosen() ? "&start=" + start + "&end=" + end : "");
    }

    /** First cars of the catalogue, for the home page. */
    public List<Car> getFeatured() {
        return cars.size() <= 6 ? cars : cars.subList(0, 6);
    }

    public int getCarCount() { return cars.size(); }

    public List<Car> getCars() { return cars; }
    public List<String> getCategories() { return categories; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public LocalDate getStart() { return start; }
    public void setStart(LocalDate start) { this.start = start; }
    public LocalDate getEnd() { return end; }
    public void setEnd(LocalDate end) { this.end = end; }
}
