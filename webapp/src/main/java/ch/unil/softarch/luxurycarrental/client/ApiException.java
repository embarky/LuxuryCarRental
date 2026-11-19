package ch.unil.softarch.luxurycarrental.client;

/** A failed API call; the message is the API's explanation, ready to show to the user. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    /** The API no longer accepts the session's token (expired, or the server restarted). */
    public boolean isUnauthorized() {
        return status == 401;
    }
}
