package ch.unil.softarch.luxurycarrental.it;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** A very small JSON client for the REST API. */
final class Http {

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** An HTTP answer: status code and body. */
    record Response(int status, String body) {

        JsonObject json() {
            return (JsonObject) parse();
        }

        JsonArray array() {
            return (JsonArray) parse();
        }

        String message() {
            return json().getString("message", "");
        }

        private JsonStructure parse() {
            try (var reader = Json.createReader(new StringReader(body))) {
                return reader.read();
            }
        }

        @Override
        public String toString() {
            return status + " " + body;
        }
    }

    private Http() {
    }

    static Response get(String path, String token) {
        return send("GET", path, null, token);
    }

    static Response post(String path, JsonValue body, String token) {
        return send("POST", path, body, token);
    }

    static Response put(String path, JsonValue body, String token) {
        return send("PUT", path, body, token);
    }

    static Response delete(String path, String token) {
        return send("DELETE", path, null, token);
    }

    static Response send(String method, String path, JsonValue body, String token) {
        return send(method, path, body, token, "application/json");
    }

    /** Downloads a file (any content type). */
    static Response download(String path, String token) {
        return send("GET", path, null, token, "*/*");
    }

    static Response send(String method, String path, JsonValue body, String token, String accept) {
        String url = path.startsWith("http") ? path : TestEnvironment.apiUrl() + path;
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", accept)
                .header("Content-Type", "application/json")
                .method(method, body == null
                        ? (method.equals("GET") || method.equals("DELETE")
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString("{}"))
                        : HttpRequest.BodyPublishers.ofString(body.toString()));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse<String> response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException(method + " " + url + " failed", e);
        }
    }
}
