package ch.unil.softarch.luxurycarrental.domain.dto;

/** A 6-digit code received by email, e.g. to confirm the email address. */
public class CodeRequest {
    public String code;

    public CodeRequest() {
    }

    public CodeRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
