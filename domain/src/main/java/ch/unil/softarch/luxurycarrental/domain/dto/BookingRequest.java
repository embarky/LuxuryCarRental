package ch.unil.softarch.luxurycarrental.domain.dto;

import java.time.LocalDate;
import java.util.UUID;

public class BookingRequest {
    public UUID customerId;
    public UUID carId;
    public LocalDate startDate;
    public LocalDate endDate;

    public BookingRequest() {
    }

    public BookingRequest(UUID customerId, UUID carId, LocalDate startDate, LocalDate endDate) {
        this.customerId = customerId;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
