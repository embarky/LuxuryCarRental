package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.api.Errors;
import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Car models (brand, model, specifications) that physical cars belong to. */
@ApplicationScoped
public class CarTypeService {

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    @Transactional
    public CarType add(CarType carType) {
        validate(carType);
        carType.setId(null);
        em.persist(carType);
        return carType;
    }

    public CarType get(UUID id) {
        CarType carType = em.find(CarType.class, id);
        if (carType == null) {
            throw Errors.notFound("Car type not found.");
        }
        return carType;
    }

    public List<CarType> getAll() {
        return em.createQuery("SELECT ct FROM CarType ct ORDER BY ct.brand, ct.model", CarType.class).getResultList();
    }

    /** Distinct categories, for the search filter. */
    public List<String> getCategories() {
        return em.createQuery("SELECT DISTINCT ct.category FROM CarType ct ORDER BY ct.category", String.class)
                .getResultList();
    }

    @Transactional
    public CarType update(UUID id, CarType update) {
        CarType existing = get(id);
        validate(update);
        existing.setCategory(update.getCategory().trim());
        existing.setBrand(update.getBrand().trim());
        existing.setModel(update.getModel().trim());
        existing.setEngine(update.getEngine());
        existing.setPower(update.getPower());
        existing.setMaxSpeed(update.getMaxSpeed());
        existing.setAcceleration(update.getAcceleration());
        existing.setWeight(update.getWeight());
        existing.setDriveType(update.getDriveType());
        existing.setTransmission(update.getTransmission());
        existing.setSeats(update.getSeats());
        existing.setDescription(update.getDescription());
        existing.setFeatures(update.getFeatures() == null ? new ArrayList<>() : new ArrayList<>(update.getFeatures()));
        return existing;
    }

    @Transactional
    public void remove(UUID id) {
        CarType existing = get(id);
        Long cars = em.createQuery("SELECT COUNT(c) FROM Car c WHERE c.carType.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        if (cars > 0) {
            throw DomainException.conflict(cars + " car(s) still use this car type. Delete or move them first.");
        }
        em.remove(existing);
    }

    private static void validate(CarType t) {
        if (t == null) {
            throw new DomainException("Car type data is missing.");
        }
        Validation.required(t.getBrand(), "Brand");
        Validation.required(t.getModel(), "Model");
        Validation.required(t.getCategory(), "Category");
        if (t.getSeats() < 1 || t.getSeats() > 9) {
            throw new DomainException("Seats must be between 1 and 9.");
        }
        if (t.getPower() < 0 || t.getMaxSpeed() < 0 || t.getAcceleration() < 0 || t.getWeight() < 0) {
            throw new DomainException("Power, speed, acceleration and weight cannot be negative.");
        }
    }
}
