package ch.unil.softarch.luxurycarrental.service;

/**
 * An email to send once the current transaction has committed. Services fire it instead of
 * sending directly, so a rolled-back change never produces an email.
 */
public record MailEvent(String to, EmailTemplates.Email email, byte[] pdf, String pdfName) {

    public static MailEvent of(String to, EmailTemplates.Email email) {
        return new MailEvent(to, email, null, null);
    }
}
