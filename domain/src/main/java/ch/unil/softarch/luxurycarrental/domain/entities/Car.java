package ch.unil.softarch.luxurycarrental.domain.entities;

import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import ch.unil.softarch.luxurycarrental.domain.model.Money;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A physical car of the fleet, linked to a {@link CarType} (brand, model, specs).
 * <p>
 * {@link #getStatus()} only says whether the car is in service. Whether it is free on
 * given dates is computed from its bookings.
 * </p>
 */
@Entity
@Table(name = "car")
public class Car implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "car_type_id", nullable = false)
    private CarType carType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRentalPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CarStatus status = CarStatus.AVAILABLE;

    @Column(length = 1000)
    private String imageUrl;

    private LocalDate registrationDate;

    private LocalDate lastMaintenanceDate;

    private String vin;

    private String color;

    private LocalDate insuranceExpiryDate;

    /** Optimistic locking: two concurrent changes to the same car cannot both win. */
    @Version
    private long version;

    public Car() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        dailyRentalPrice = Money.of(dailyRentalPrice);
        depositAmount = Money.of(depositAmount);
    }

    /** True if the car is in service and can be booked for free dates. */
    public boolean isInService() {
        return status == CarStatus.AVAILABLE;
    }

    public String getDisplayName() {
        return carType == null ? licensePlate : carType.getBrand() + " " + carType.getModel();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public CarType getCarType() { return carType; }
    public void setCarType(CarType carType) { this.carType = carType; }

    public BigDecimal getDailyRentalPrice() { return dailyRentalPrice; }
    public void setDailyRentalPrice(BigDecimal dailyRentalPrice) { this.dailyRentalPrice = dailyRentalPrice; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public CarStatus getStatus() { return status; }
    public void setStatus(CarStatus status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }

    public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public LocalDate getInsuranceExpiryDate() { return insuranceExpiryDate; }
    public void setInsuranceExpiryDate(LocalDate insuranceExpiryDate) { this.insuranceExpiryDate = insuranceExpiryDate; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Car other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Car{id=" + id + ", plate='" + licensePlate + "', " + getDisplayName() + ", status=" + status + "}";
    }
}
