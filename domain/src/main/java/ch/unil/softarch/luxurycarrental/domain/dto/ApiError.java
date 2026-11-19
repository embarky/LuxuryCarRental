package ch.unil.softarch.luxurycarrental.domain.dto;

/** Error body returned by the API for every failed request. */
public class ApiError {
    public int status;
    public String message;

    public ApiError() {
    }

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
