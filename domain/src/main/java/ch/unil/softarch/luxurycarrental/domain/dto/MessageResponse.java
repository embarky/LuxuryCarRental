package ch.unil.softarch.luxurycarrental.domain.dto;

/** A short confirmation such as "Verification code sent". */
public class MessageResponse {
    public String message;

    public MessageResponse() {
    }

    public MessageResponse(String message) {
        this.message = message;
    }
}
