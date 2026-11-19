package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.api.Errors;
import ch.unil.softarch.luxurycarrental.domain.DomainException;
import ch.unil.softarch.luxurycarrental.domain.dto.BookedPeriod;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import ch.unil.softarch.luxurycarrental.domain.model.DateRange;
import ch.unil.softarch.luxurycarrental.domain.model.Money;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The fleet: cars, their search and their availability on given dates. */
@ApplicationScoped
public class CarService {

    /** Booking states that occupy a car (see {@link BookingStatus#blocksCar()}). */
    static final List<BookingStatus> BLOCKING =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);

    @PersistenceContext(unitName = "LuxuryCarRentalPU")
    private EntityManager em;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Car get(UUID id) {
        Car car = em.find(Car.class, id);
        if (car == null) {
            throw Errors.notFound("Car not found.");
        }
        return car;
    }

    /** Every car, including those in maintenance (admin view). */
    public List<Car> getAll() {
        return em.createQuery("SELECT c FROM Car c ORDER BY c.carType.brand, c.carType.model, c.licensePlate", Car.class)
                .getResultList();
    }

    /**
     * Cars customers can book, filtered by any combination of the criteria. With both dates,
     * only cars free for the whole period are returned.
     *
     * @param text      matched against brand, model, category and colour
     * @param category  exact category, e.g. "SUV"
     */
    public List<Car> search(String text, UUID carTypeId, String category, BigDecimal minPrice, BigDecimal maxPrice,
                            LocalDate start, LocalDate end) {
        StringBuilder jpql = new StringBuilder("SELECT c FROM Car c WHERE c.status = :inService");
        Map<String, Object> params = new HashMap<>();
        params.put("inService", CarStatus.AVAILABLE);

        if (text != null && !text.isBlank()) {
            jpql.append(" AND (LOWER(c.carType.brand) LIKE :text OR LOWER(c.carType.model) LIKE :text"
                    + " OR LOWER(c.carType.category) LIKE :text OR LOWER(c.color) LIKE :text)");
            params.put("text", "%" + text.trim().toLowerCase() + "%");
        }
        if (carTypeId != null) {
            jpql.append(" AND c.carType.id = :typeId");
            params.put("typeId", carTypeId);
        }
        if (category != null && !category.isBlank()) {
            jpql.append(" AND c.carType.category = :category");
            params.put("category", category.trim());
        }
        if (minPrice != null) {
            jpql.append(" AND c.dailyRentalPrice >= :minPrice");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            jpql.append(" AND c.dailyRentalPrice <= :maxPrice");
            params.put("maxPrice", maxPrice);
        }
        if (start != null || end != null) {
            DateRange period = new DateRange(start, end);
            jpql.append(" AND NOT EXISTS (SELECT b FROM Booking b WHERE b.car = c AND b.bookingStatus IN :blocking"
                    + " AND b.startDate < :end AND :start < b.endDate)");
            params.put("blocking", BLOCKING);
            params.put("start", period.start());
            params.put("end", period.end());
        }
        jpql.append(" ORDER BY c.dailyRentalPrice");

        TypedQuery<Car> query = em.createQuery(jpql.toString(), Car.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    /** Future periods during which the car is taken, without any customer data. */
    public List<BookedPeriod> bookedPeriods(UUID carId) {
        get(carId);
        return em.createQuery("SELECT b.startDate, b.endDate FROM Booking b WHERE b.car.id = :id"
                        + " AND b.bookingStatus IN :blocking AND b.endDate > :today ORDER BY b.startDate", Object[].class)
                .setParameter("id", carId)
                .setParameter("blocking", BLOCKING)
                .setParameter("today", LocalDate.now())
                .getResultList()
                .stream()
                .map(row -> new BookedPeriod((LocalDate) row[0], (LocalDate) row[1]))
                .toList();
    }

    /** Number of bookings that occupy the car during the period (0 = free). */
    long overlappingBookings(UUID carId, DateRange period) {
        return em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.car.id = :id AND b.bookingStatus IN :blocking"
                        + " AND b.startDate < :end AND :start < b.endDate", Long.class)
                .setParameter("id", carId)
                .setParameter("blocking", BLOCKING)
                .setParameter("start", period.start())
                .setParameter("end", period.end())
                .getSingleResult();
    }

    // -------------------------------------------------------------------------
    // Changes (administrators)
    // -------------------------------------------------------------------------

    @Transactional
    public Car add(Car car) {
        if (car == null) {
            throw new DomainException("Car data is missing.");
        }
        Car created = new Car();
        apply(created, car);                 // checks the plate before the new car has an id
        created.setId(UUID.randomUUID());
        em.persist(created);
        return created;
    }

    @Transactional
    public Car update(UUID id, Car update) {
        Car existing = get(id);
        if (update == null) {
            throw new DomainException("Car data is missing.");
        }
        apply(existing, update);
        return existing;
    }

    /** Cars with bookings stay in the database for the history; set them to maintenance instead. */
    @Transactional
    public void remove(UUID id) {
        Car car = get(id);
        Long bookings = em.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.car.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
        if (bookings > 0) {
            throw DomainException.conflict("This car has " + bookings + " booking(s) in its history and cannot be "
                    + "deleted. Set it to maintenance to stop new bookings.");
        }
        em.remove(car);
    }

    private void apply(Car target, Car source) {
        String plate = Validation.required(source.getLicensePlate(), "Licence plate").toUpperCase();
        TypedQuery<Long> plateQuery = em.createQuery("SELECT COUNT(c) FROM Car c WHERE UPPER(c.licensePlate) = :plate"
                        + (target.getId() == null ? "" : " AND c.id <> :id"), Long.class)
                .setParameter("plate", plate);
        if (target.getId() != null) {
            plateQuery.setParameter("id", target.getId());
        }
        if (plateQuery.getSingleResult() > 0) {
            throw DomainException.conflict("Another car already has the licence plate " + plate + ".");
        }
        if (source.getCarType() == null || source.getCarType().getId() == null) {
            throw new DomainException("Please choose a car type.");
        }
        CarType type = em.find(CarType.class, source.getCarType().getId());
        if (type == null) {
            throw new DomainException("The selected car type does not exist.");
        }
        target.setLicensePlate(plate);
        target.setCarType(type);
        target.setDailyRentalPrice(Money.requirePositive(source.getDailyRentalPrice(), "The daily price"));
        target.setDepositAmount(Money.requirePositive(source.getDepositAmount(), "The deposit"));
        target.setStatus(source.getStatus() == null ? CarStatus.AVAILABLE : source.getStatus());
        target.setColor(Validation.optional(source.getColor()));
        target.setVin(Validation.optional(source.getVin()));
        target.setImageUrl(Validation.optional(source.getImageUrl()));
        target.setRegistrationDate(source.getRegistrationDate());
        target.setLastMaintenanceDate(source.getLastMaintenanceDate());
        target.setInsuranceExpiryDate(source.getInsuranceExpiryDate());
    }

    /** Cars that are in service, for the home page. */
    public List<Car> inService(int limit) {
        List<Car> cars = new ArrayList<>(em.createQuery("SELECT c FROM Car c WHERE c.status = :s ORDER BY c.dailyRentalPrice DESC", Car.class)
                .setParameter("s", CarStatus.AVAILABLE)
                .setMaxResults(limit)
                .getResultList());
        return cars;
    }
}
