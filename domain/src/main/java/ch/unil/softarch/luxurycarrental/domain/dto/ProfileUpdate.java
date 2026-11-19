package ch.unil.softarch.luxurycarrental.domain.dto;

import java.time.LocalDate;

/** Fields a customer can change in their account. Null fields are left unchanged. */
public class ProfileUpdate {
    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String billingAddress;
    public String drivingLicenseNumber;
    public LocalDate drivingLicenseExpiryDate;
}
