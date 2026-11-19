package ch.unil.softarch.luxurycarrental.client;

import ch.unil.softarch.luxurycarrental.domain.dto.*;
import ch.unil.softarch.luxurycarrental.domain.entities.*;
import ch.unil.softarch.luxurycarrental.domain.enums.BookingStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.GenericType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every call the web layer makes to the REST API, in one place.
 * Each method maps to one endpoint of the service layer.
 */
@ApplicationScoped
public class Api {

    private static final GenericType<Customer> CUSTOMER = new GenericType<>() {};
    private static final GenericType<List<Customer>> CUSTOMERS = new GenericType<>() {};
    private static final GenericType<Admin> ADMIN = new GenericType<>() {};
    private static final GenericType<List<Admin>> ADMINS = new GenericType<>() {};
    private static final GenericType<Car> CAR = new GenericType<>() {};
    private static final GenericType<List<Car>> CARS = new GenericType<>() {};
    private static final GenericType<CarType> CAR_TYPE = new GenericType<>() {};
    private static final GenericType<List<CarType>> CAR_TYPES = new GenericType<>() {};
    private static final GenericType<List<String>> STRINGS = new GenericType<>() {};
    private static final GenericType<Booking> BOOKING = new GenericType<>() {};
    private static final GenericType<List<Booking>> BOOKINGS = new GenericType<>() {};
    private static final GenericType<List<BookedPeriod>> PERIODS = new GenericType<>() {};
    private static final GenericType<MessageResponse> MESSAGE = new GenericType<>() {};

    // ---- customers ----------------------------------------------------------

    public CustomerSession loginCustomer(String email, String password) {
        return ApiClient.post("customers/login", new LoginRequest(email, password), new GenericType<>() {});
    }

    public Customer register(RegistrationRequest request) {
        return ApiClient.post("customers", request, CUSTOMER);
    }

    public Customer customer(UUID id) {
        return ApiClient.get("customers/" + id, CUSTOMER);
    }

    public List<Customer> customers() {
        return ApiClient.get("customers", CUSTOMERS);
    }

    public Customer updateProfile(UUID id, ProfileUpdate update) {
        return ApiClient.put("customers/" + id, update, CUSTOMER);
    }

    public Customer topUp(UUID id, BigDecimal amount) {
        return ApiClient.post("customers/" + id + "/top-up", new AmountRequest(amount), CUSTOMER);
    }

    public String changeCustomerPassword(UUID id, PasswordChange change) {
        return ApiClient.put("customers/" + id + "/password", change, MESSAGE).message;
    }

    public Customer setVerified(UUID id, boolean verified) {
        return ApiClient.put("customers/" + id + "/verified", Map.of("value", verified), null, CUSTOMER);
    }

    public Customer confirmEmail(UUID id, String code) {
        return ApiClient.put("customers/" + id + "/email-confirmation", new CodeRequest(code), CUSTOMER);
    }

    public String sendEmailCode(UUID id) {
        return ApiClient.post("customers/" + id + "/email-confirmation", null, MESSAGE).message;
    }

    public Customer setEmailVerified(UUID id, boolean verified) {
        return ApiClient.put("customers/" + id + "/email-verified", Map.of("value", verified), null, CUSTOMER);
    }

    public void deleteCustomer(UUID id) {
        ApiClient.delete("customers/" + id);
    }

    public String sendResetCode(boolean admin, String email) {
        PasswordReset r = new PasswordReset();
        r.email = email;
        return ApiClient.post((admin ? "admins" : "customers") + "/password-reset-code", r, MESSAGE).message;
    }

    public String resetPassword(boolean admin, String email, String code, String newPassword) {
        PasswordReset r = new PasswordReset();
        r.email = email;
        r.code = code;
        r.newPassword = newPassword;
        return ApiClient.put((admin ? "admins" : "customers") + "/reset-password", r, MESSAGE).message;
    }

    // ---- administrators -----------------------------------------------------

    public AdminSession loginAdmin(String email, String password) {
        return ApiClient.post("admins/login", new LoginRequest(email, password), new GenericType<>() {});
    }

    public List<Admin> admins() {
        return ApiClient.get("admins", ADMINS);
    }

    public Admin createAdmin(AdminRequest request) {
        return ApiClient.post("admins", request, ADMIN);
    }

    public void deleteAdmin(UUID id) {
        ApiClient.delete("admins/" + id);
    }

    public String changeOwnAdminPassword(PasswordChange change) {
        return ApiClient.put("admins/me/password", change, MESSAGE).message;
    }

    // ---- car types ----------------------------------------------------------

    public List<CarType> carTypes() {
        return ApiClient.get("cartypes", CAR_TYPES);
    }

    public List<String> categories() {
        return ApiClient.get("cartypes/categories", STRINGS);
    }

    public CarType carType(UUID id) {
        return ApiClient.get("cartypes/" + id, CAR_TYPE);
    }

    public CarType saveCarType(CarType type) {
        return type.getId() == null
                ? ApiClient.post("cartypes", type, CAR_TYPE)
                : ApiClient.put("cartypes/" + type.getId(), type, CAR_TYPE);
    }

    public void deleteCarType(UUID id) {
        ApiClient.delete("cartypes/" + id);
    }

    // ---- cars ---------------------------------------------------------------

    /** Every car, including those in maintenance (admin). */
    public List<Car> allCars() {
        return ApiClient.get("cars", CARS);
    }

    /** Bookable cars matching the filters (any of them may be null). */
    public List<Car> searchCars(String text, String category, BigDecimal maxPrice, LocalDate start, LocalDate end) {
        Map<String, Object> q = new HashMap<>();
        q.put("q", text);
        q.put("category", category);
        q.put("maxPrice", maxPrice);
        q.put("start", start);
        q.put("end", end);
        return ApiClient.get("cars/search", q, CARS);
    }

    public Car car(UUID id) {
        return ApiClient.get("cars/" + id, CAR);
    }

    public List<BookedPeriod> bookedPeriods(UUID carId) {
        return ApiClient.get("cars/" + carId + "/booked", PERIODS);
    }

    public Car saveCar(Car car) {
        return car.getId() == null
                ? ApiClient.post("cars", car, CAR)
                : ApiClient.put("cars/" + car.getId(), car, CAR);
    }

    public void deleteCar(UUID id) {
        ApiClient.delete("cars/" + id);
    }

    // ---- bookings -----------------------------------------------------------

    public List<Booking> bookings(BookingStatus status) {
        Map<String, Object> q = new HashMap<>();
        q.put("status", status);
        return ApiClient.get("bookings", q, BOOKINGS);
    }

    public List<Booking> bookingsOfCustomer(UUID customerId) {
        return ApiClient.get("bookings/customer/" + customerId, BOOKINGS);
    }

    public Booking book(UUID customerId, UUID carId, LocalDate start, LocalDate end) {
        return ApiClient.post("bookings", new BookingRequest(customerId, carId, start, end), BOOKING);
    }

    public Booking pay(UUID bookingId) {
        return ApiClient.post("bookings/" + bookingId + "/pay", null, BOOKING);
    }

    public Booking cancel(UUID bookingId) {
        return ApiClient.post("bookings/" + bookingId + "/cancel", null, BOOKING);
    }

    public Booking reject(UUID bookingId, String reason) {
        return ApiClient.post("bookings/" + bookingId + "/reject", new ReasonRequest(reason), BOOKING);
    }

    public Booking pickUp(UUID bookingId) {
        return ApiClient.post("bookings/" + bookingId + "/pick-up", null, BOOKING);
    }

    public Booking complete(UUID bookingId) {
        return ApiClient.post("bookings/" + bookingId + "/complete", null, BOOKING);
    }

    public byte[] bookingPdf(UUID bookingId, String token) {
        return ApiClient.bytes("bookings/" + bookingId + "/pdf", token);
    }
}
