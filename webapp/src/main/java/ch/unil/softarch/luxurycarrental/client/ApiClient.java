package ch.unil.softarch.luxurycarrental.client;

import ch.unil.softarch.luxurycarrental.domain.dto.ApiError;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Thin wrapper around the JAX-RS client used by every REST client of the web layer:
 * adds the logged-in user's token, and turns error responses into {@link ApiException}
 * carrying the API's message.
 */
public final class ApiClient {

    private static final Client CLIENT = ClientBuilder.newClient();

    private ApiClient() {
    }

    public static <T> T get(String path, GenericType<T> type) {
        return get(path, null, type);
    }

    public static <T> T get(String path, Map<String, Object> query, GenericType<T> type) {
        return read(() -> request(path, query).get(), type);
    }

    public static <T> T post(String path, Object body, GenericType<T> type) {
        return read(() -> request(path, null).post(json(body)), type);
    }

    public static <T> T put(String path, Object body, GenericType<T> type) {
        return put(path, null, body, type);
    }

    public static <T> T put(String path, Map<String, Object> query, Object body, GenericType<T> type) {
        return read(() -> request(path, query).put(json(body)), type);
    }

    public static void delete(String path) {
        read(() -> request(path, null).delete(), null);
    }

    /** Raw bytes (e.g. a PDF), with an explicit token for calls made outside a JSF request. */
    public static byte[] bytes(String path, String token) {
        Invocation.Builder builder = target(path, null).request();
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return read(builder::get, new GenericType<byte[]>() {});
    }

    // -------------------------------------------------------------------------

    private static WebTarget target(String path, Map<String, Object> query) {
        WebTarget target = CLIENT.target(ApiConfig.API_BASE_URL).path(path);
        if (query != null) {
            for (Map.Entry<String, Object> e : query.entrySet()) {
                if (e.getValue() != null && !e.getValue().toString().isBlank()) {
                    target = target.queryParam(e.getKey(), e.getValue());
                }
            }
        }
        return target;
    }

    private static Invocation.Builder request(String path, Map<String, Object> query) {
        Invocation.Builder builder = target(path, query).request(MediaType.APPLICATION_JSON);
        String token = ApiTokens.current();
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return builder;
    }

    private static Entity<?> json(Object body) {
        return Entity.entity(body == null ? "{}" : body, MediaType.APPLICATION_JSON);
    }

    private static <T> T read(Supplier<Response> call, GenericType<T> type) {
        try (Response response = call.get()) {
            if (response.getStatus() >= 400) {
                throw new ApiException(response.getStatus(), message(response));
            }
            return type == null || response.getStatus() == 204 ? null : response.readEntity(type);
        } catch (ProcessingException e) {
            throw new ApiException(503, "The booking service is not reachable right now. Please try again.");
        }
    }

    private static String message(Response response) {
        try {
            ApiError error = response.readEntity(ApiError.class);
            if (error != null && error.message != null) {
                return error.message;
            }
        } catch (RuntimeException ignored) {
            // not a JSON error body
        }
        return switch (response.getStatus()) {
            case 401 -> "Please log in again.";
            case 403 -> "You are not allowed to do this.";
            case 404 -> "Not found.";
            default -> "Something went wrong (HTTP " + response.getStatus() + ").";
        };
    }
}
