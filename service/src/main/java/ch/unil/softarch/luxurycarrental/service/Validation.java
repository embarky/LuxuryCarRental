package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.domain.DomainException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Small input checks shared by the services; each error message is shown to the user. */
final class Validation {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");

    private Validation() {
    }

    static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required.");
        }
        return value.trim();
    }

    static <T> T required(T value, String field) {
        if (value == null) {
            throw new DomainException(field + " is required.");
        }
        return value;
    }

    static String email(String value) {
        String email = required(value, "Email").toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new DomainException("Please enter a valid email address.");
        }
        return email;
    }

    static String phone(String value) {
        String phone = required(value, "Phone number");
        if (!PHONE.matcher(phone).matches()) {
            throw new DomainException("Please enter a valid phone number, e.g. +41 79 123 45 67.");
        }
        return phone;
    }

    static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
