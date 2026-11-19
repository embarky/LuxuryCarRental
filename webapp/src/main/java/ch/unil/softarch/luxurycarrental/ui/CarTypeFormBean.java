package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import ch.unil.softarch.luxurycarrental.domain.enums.DriveType;
import ch.unil.softarch.luxurycarrental.domain.enums.Transmission;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/** Add or edit a car type ({@code ?id=} to edit). */
@Named("carTypeFormBean")
@ViewScoped
public class CarTypeFormBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private String id;
    private CarType carType;
    private String features;

    public void load() {
        if (FacesContext.getCurrentInstance().isPostback()) {
            return;
        }
        try {
            if (id != null && !id.isBlank()) {
                carType = api.carType(UUID.fromString(id));
                features = carType.getFeatures() == null ? "" : String.join(", ", carType.getFeatures());
            } else {
                carType = new CarType();
                carType.setSeats(4);
                carType.setDriveType(DriveType.REAR_WHEEL_DRIVE);
                carType.setTransmission(Transmission.AUTOMATIC);
                features = "";
            }
        } catch (ApiException e) {
            Messages.error(e);
        } catch (IllegalArgumentException e) {
            Messages.error("This car type does not exist.");
        }
    }

    public String save() {
        carType.setFeatures(features == null || features.isBlank()
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.stream(features.split(",")).map(String::trim).filter(f -> !f.isEmpty()).toList()));
        try {
            CarType saved = api.saveCarType(carType);
            Messages.info("Saved " + saved.getBrand() + " " + saved.getModel() + ".");
            Messages.keep();
            return "/pages/admin/car_type.xhtml?faces-redirect=true";
        } catch (ApiException e) {
            Messages.error(e);
            return null;
        }
    }

    public boolean isEditing() {
        return carType != null && carType.getId() != null;
    }

    public DriveType[] getDriveTypes() { return DriveType.values(); }
    public Transmission[] getTransmissions() { return Transmission.values(); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public CarType getCarType() { return carType; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
}
