package ch.unil.softarch.luxurycarrental.domain.dto;

import java.time.LocalDate;

/** Data a visitor fills in to create a customer account. */
public class RegistrationRequest {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String phoneNumber;
    public LocalDate dateOfBirth;
    public String drivingLicenseNumber;
    public LocalDate drivingLicenseExpiryDate;
    public String billingAddress;
}
