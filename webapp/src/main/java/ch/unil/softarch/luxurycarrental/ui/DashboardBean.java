package ch.unil.softarch.luxurycarrental.ui;

import ch.unil.softarch.luxurycarrental.client.Api;
import ch.unil.softarch.luxurycarrental.client.ApiException;
import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import ch.unil.softarch.luxurycarrental.domain.enums.CarStatus;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard. Definitions:
 * <ul>
 *     <li>revenue = rental (not deposits) of bookings paid in the month and not refunded;</li>
 *     <li>utilisation = cars in use or reserved today / cars in service;</li>
 *     <li>to do = unpaid bookings, pick-ups due, returns due, customers to verify.</li>
 * </ul>
 */
@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Api api;

    private int carsInService;
    private int carsInMaintenance;
    private int customers;
    private int customersToVerify;
    private BigDecimal revenueThisMonth = BigDecimal.ZERO;
    private BigDecimal revenueLastMonth = BigDecimal.ZERO;
    private int carsInUseToday;
    private final Map<BookingStatus, Integer> byStatus = new EnumMap<>(BookingStatus.class);
    private List<Booking> pickUpsToday = Collections.emptyList();
    private List<Booking> returnsDue = Collections.emptyList();
    private List<Booking> recent = Collections.emptyList();

    @PostConstruct
    public void init() {
        try {
            List<Car> cars = api.allCars();
            List<Customer> allCustomers = api.customers();
            List<Booking> bookings = api.bookings(null);
            LocalDate today = LocalDate.now();

            carsInService = (int) cars.stream().filter(c -> c.getStatus() == CarStatus.AVAILABLE).count();
            carsInMaintenance = cars.size() - carsInService;
            customers = allCustomers.size();
            customersToVerify = (int) allCustomers.stream().filter(c -> !c.isVerifiedIdentity()).count();

            for (BookingStatus s : BookingStatus.values()) {
                byStatus.put(s, 0);
            }
            bookings.forEach(b -> byStatus.merge(b.getBookingStatus(), 1, Integer::sum));

            revenueThisMonth = revenue(bookings, YearMonth.now());
            revenueLastMonth = revenue(bookings, YearMonth.now().minusMonths(1));

            carsInUseToday = (int) bookings.stream()
                    .filter(b -> b.getBookingStatus() == BookingStatus.ACTIVE
                            || (b.getBookingStatus() == BookingStatus.CONFIRMED
                            && !b.getStartDate().isAfter(today) && b.getEndDate().isAfter(today)))
                    .map(b -> b.getCar().getId()).distinct().count();

            pickUpsToday = bookings.stream()
                    .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED && !b.getStartDate().isAfter(today))
                    .sorted(Comparator.comparing(Booking::getStartDate)).toList();
            returnsDue = bookings.stream()
                    .filter(b -> b.getBookingStatus() == BookingStatus.ACTIVE && !b.getEndDate().isAfter(today))
                    .sorted(Comparator.comparing(Booking::getEndDate)).toList();
            recent = bookings.size() <= 8 ? bookings : bookings.subList(0, 8);
        } catch (ApiException e) {
            Messages.error(e);
        }
    }

    private static BigDecimal revenue(List<Booking> bookings, YearMonth month) {
        return bookings.stream()
                .filter(b -> b.getPaidAt() != null && YearMonth.from(b.getPaidAt()).equals(month))
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED
                        || b.getBookingStatus() == BookingStatus.ACTIVE
                        || b.getBookingStatus() == BookingStatus.COMPLETED)
                .map(Booking::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Utilisation today in percent. */
    public int getUtilisation() {
        return carsInService == 0 ? 0 : Math.round(100f * carsInUseToday / carsInService);
    }

    /** Status rows with their count and bar width (percent of the largest), for the chart. */
    public List<StatusRow> getStatusRows() {
        int max = byStatus.values().stream().max(Integer::compare).orElse(0);
        List<StatusRow> rows = new ArrayList<>();
        byStatus.forEach((s, n) -> rows.add(new StatusRow(s, n, max == 0 ? 0 : Math.max(2, 100 * n / max))));
        return rows;
    }

    public record StatusRow(BookingStatus status, int count, int width) {
        public BookingStatus getStatus() { return status; }
        public int getCount() { return count; }
        public int getWidth() { return width; }
    }

    public int getCarsInService() { return carsInService; }
    public int getCarsInMaintenance() { return carsInMaintenance; }
    public int getCustomers() { return customers; }
    public int getCustomersToVerify() { return customersToVerify; }
    public BigDecimal getRevenueThisMonth() { return revenueThisMonth; }
    public BigDecimal getRevenueLastMonth() { return revenueLastMonth; }
    public int getCarsInUseToday() { return carsInUseToday; }
    public int getPending() { return byStatus.getOrDefault(BookingStatus.PENDING, 0); }
    public List<Booking> getPickUpsToday() { return pickUpsToday; }
    public List<Booking> getReturnsDue() { return returnsDue; }
    public List<Booking> getRecent() { return recent; }
}
