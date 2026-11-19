package ch.unil.softarch.luxurycarrental.service;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends HTML emails over SMTP.
 * <p>
 * All settings come from environment variables (see {@code .env.example}):
 * </p>
 * <ul>
 *     <li>{@code MAIL_SMTP_HOST} / {@code MAIL_SMTP_PORT} – SMTP server. With Docker Compose this is
 *     the local Mailpit server, whose web UI (http://localhost:8025) shows every email sent.
 *     If the host is empty, emails are only written to the server log.</li>
 *     <li>{@code MAIL_USERNAME} / {@code MAIL_APP_PASSWORD} – optional SMTP login, needed for a real
 *     provider such as Gmail. Authentication is used only when both are set.</li>
 *     <li>{@code MAIL_SMTP_STARTTLS} – {@code true} for providers that require TLS.</li>
 *     <li>{@code MAIL_FROM} – sender address.</li>
 * </ul>
 * Emails are sent in the background so that a slow mail server never delays an API response.
 */
public final class EmailSender {

    private static final String SMTP_HOST = env("MAIL_SMTP_HOST", "");
    private static final String SMTP_PORT = env("MAIL_SMTP_PORT", "1025");
    private static final String USERNAME = env("MAIL_USERNAME", "");
    private static final String PASSWORD = env("MAIL_APP_PASSWORD", "");
    private static final boolean STARTTLS = Boolean.parseBoolean(env("MAIL_SMTP_STARTTLS", "false"));
    private static final String FROM_ADDRESS = env("MAIL_FROM",
            USERNAME.isEmpty() ? "no-reply@luxurycarrental.local" : USERNAME);
    private static final String FROM_NAME = "Luxury Car Rental";

    /** Fallback when the server's managed executor cannot be looked up (e.g. in unit tests). */
    private static final ExecutorService FALLBACK_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "email-sender");
        thread.setDaemon(true);
        return thread;
    });

    private EmailSender() {
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Sends an HTML email in the background. */
    public static void send(String to, EmailTemplates.Email email) {
        executor().execute(() -> deliver(to, email, null, null));
    }

    /** Sends an HTML email with a PDF attachment in the background. */
    public static void sendWithPdf(String to, EmailTemplates.Email email, byte[] pdf, String fileName) {
        executor().execute(() -> deliver(to, email, pdf, fileName));
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    private static void deliver(String to, EmailTemplates.Email email, byte[] pdf, String fileName) {
        if (SMTP_HOST.isEmpty()) {
            System.out.println("[EmailSender] MAIL_SMTP_HOST not set - email not sent. To: " + to
                    + " | Subject: " + email.subject());
            return;
        }

        try {
            MimeMessage message = new MimeMessage(createSession());
            message.setFrom(new InternetAddress(FROM_ADDRESS, FROM_NAME, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(email.subject(), StandardCharsets.UTF_8.name());

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(email.html(), "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);

            if (pdf != null && pdf.length > 0) {
                MimeBodyPart attachment = new MimeBodyPart();
                attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(pdf, "application/pdf")));
                attachment.setFileName(fileName);
                multipart.addBodyPart(attachment);
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("[EmailSender] Sent \"" + email.subject() + "\" to " + to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("[EmailSender] Failed to send \"" + email.subject() + "\" to " + to
                    + ": " + e.getMessage());
        }
    }

    private static Session createSession() {
        boolean auth = !USERNAME.isEmpty() && !PASSWORD.isEmpty();

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(STARTTLS));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        if (!auth) {
            return Session.getInstance(props);
        }
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
    }

    /**
     * Uses the application server's managed executor, so the background work runs on
     * container-managed threads instead of threads created by the application.
     */
    private static ExecutorService executor() {
        try {
            return (ExecutorService) new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
        } catch (NamingException | ClassCastException e) {
            return FALLBACK_EXECUTOR;
        }
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }
}
