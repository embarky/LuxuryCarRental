package ch.unil.softarch.luxurycarrental.client;

/**
 * Central place for the REST API base URL used by all clients.
 * <p>
 * Override with the environment variable {@code API_BASE_URL} when the
 * service layer runs on another host/port.
 * </p>
 */
public final class ApiConfig {

    public static final String API_BASE_URL = resolveBaseUrl();

    private ApiConfig() {
    }

    private static String resolveBaseUrl() {
        String value = System.getenv("API_BASE_URL");
        if (value == null || value.isBlank()) {
            value = "http://localhost:8080/luxurycarrental/api";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
