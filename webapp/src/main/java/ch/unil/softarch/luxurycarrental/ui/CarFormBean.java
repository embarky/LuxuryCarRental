package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Add or edit a car ({@code ?id=} to edit). */
@Named("carFormBean")
@ViewScoped
public class CarFormBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String id;
    private Car car;
    private UUID carTypeId;
    private List<CarType> carTypes = Collections.emptyList();

    public void load() {
        if (FacesContext.getCurrentInstance().isPostback()) {
            return;
        }
        try {
            carTypes = api.carTypes();
            if (id != null && !id.isBlank()) {
                car = api.car(UUID.fromString(id));
                carTypeId = car.getCarType().getId();
            } else {
                car = new Car();
                car.setStatus(CarStatus.AVAILABLE);
            }
        } catch (ApiException e) {
            Messages.error(e);
        } catch (IllegalArgumentException e) {
            Messages.error("This car does not exist.");
        }
    }

    public String save() {
        try {
            CarType type = new CarType();
            type.setId(carTypeId);
            car.setCarType(carTypeId == null ? null : type);
            Car saved = api.saveCar(car);
            Messages.info((isEditing() ? "Saved " : "Added ") + saved.getDisplayName() + " (" + saved.getLicensePlate() + ").");
            Messages.keep();
            return "/pages/admin/car.xhtml?faces-redirect=true";
        } catch (ApiException e) {
            Messages.error(e);
            return null;
        }
    }

    public boolean isEditing() {
        return car != null && car.getId() != null;
    }

    public CarStatus[] getStatuses() { return CarStatus.values(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Car getCar() { return car; }
    public UUID getCarTypeId() { return carTypeId; }
    public void setCarTypeId(UUID carTypeId) { this.carTypeId = carTypeId; }
    public List<CarType> getCarTypes() { return carTypes; }
}
