package ch.unil.softarch.luxurycarrental.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

/**
 * Work the server does on its own:
 * <ul>
 *     <li>at start-up, create the first administrator if there is none
 *     ({@code DEFAULT_ADMIN_EMAIL} / {@code DEFAULT_ADMIN_PASSWORD});</li>
 *     <li>every 5 minutes, expire unpaid bookings whose payment window has passed.</li>
 * </ul>
 */
@Singleton
@Startup
public class StartupTasks {

    @Inject
    private AdminService admins;

    @Inject
    private BookingService bookings;

    @PostConstruct
    void createDefaultAdmin() {
        admins.ensureDefaultAdmin(env("DEFAULT_ADMIN_EMAIL", "admin@example.com"),
                env("DEFAULT_ADMIN_PASSWORD", "admin123"));
    }

    @Schedule(minute = "*/5", hour = "*", persistent = false)
    void expireUnpaidBookings() {
        int expired = bookings.expireOverdue();
        if (expired > 0) {
            System.out.println("[StartupTasks] Expired " + expired + " unpaid booking(s).");
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
