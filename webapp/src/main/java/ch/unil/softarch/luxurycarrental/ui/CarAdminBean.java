package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Admin lists of cars and car types. */
@Named("carAdminBean")
@ViewScoped
public class CarAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private List<Car> cars = Collections.emptyList();
    private List<CarType> carTypes = Collections.emptyList();

    @PostConstruct
    public void init() {
        load();
    }

    private void load() {
        try {
            cars = api.allCars();
            carTypes = api.carTypes();
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    public void toggleMaintenance(Car car) {
        try {
            car.setStatus(car.getStatus() == CarStatus.MAINTENANCE ? CarStatus.AVAILABLE : CarStatus.MAINTENANCE);
            api.saveCar(car);
            Messages.info(car.getLicensePlate() + " is now " + new Format().carStatus(car.getStatus()).toLowerCase() + ".");
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public void deleteCar(UUID id) {
        try {
            api.deleteCar(id);
            Messages.info("Car deleted.");
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public void deleteCarType(UUID id) {
        try {
            api.deleteCarType(id);
            Messages.info("Car type deleted.");
        } catch (ApiException e) {
            Messages.error(e);
        }
        load();
    }

    public long carsOfType(CarType type) {
        return cars.stream().filter(c -> c.getCarType().getId().equals(type.getId())).count();
    }

    public List<Car> getCars() { return cars; }
    public List<CarType> getCarTypes() { return carTypes; }
}
