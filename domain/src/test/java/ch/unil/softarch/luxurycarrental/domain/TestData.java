package ch.unil.softarch.luxurycarrental.domain;

import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.CarType;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Small factory for test objects. */
final class TestData {

    static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

    private TestData() {
    }

    static Customer customer(String balance, boolean verified) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setFirstName("Ada");
        c.setLastName("Lovelace");
        c.setEmail("ada@example.com");
        c.setDateOfBirth(LocalDate.of(1990, 1, 1));
        c.setDrivingLicenseExpiryDate(LocalDate.of(2030, 1, 1));
        c.setVerifiedIdentity(verified);
        c.setBalance(new BigDecimal(balance));
        return c;
    }

    /** CHF 300 per day, CHF 500 deposit. */
    static Car car() {
        CarType type = new CarType();
        type.setBrand("Porsche");
        type.setModel("911");
        Car car = new Car();
        car.setId(UUID.randomUUID());
        car.setCarType(type);
        car.setLicensePlate("VD-1");
        car.setDailyRentalPrice(new BigDecimal("300.00"));
        car.setDepositAmount(new BigDecimal("500.00"));
        car.setStatus(CarStatus.AVAILABLE);
        return car;
    }
}
