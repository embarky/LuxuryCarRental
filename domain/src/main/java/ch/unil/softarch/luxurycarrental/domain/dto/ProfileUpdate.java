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

    // Getters and setters for JSF forms (JSON-B uses the public fields)
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public String getDrivingLicenseNumber() { return drivingLicenseNumber; }
    public void setDrivingLicenseNumber(String drivingLicenseNumber) { this.drivingLicenseNumber = drivingLicenseNumber; }
    public LocalDate getDrivingLicenseExpiryDate() { return drivingLicenseExpiryDate; }
    public void setDrivingLicenseExpiryDate(LocalDate drivingLicenseExpiryDate) { this.drivingLicenseExpiryDate = drivingLicenseExpiryDate; }
}
