package ch.unil.softarch.luxurycarrental.service;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.Properties;

public class EmailSender {

    /*
     * SMTP credentials are read from environment variables (see .env.example).
     * If they are not set, emails are not sent but printed to the server log instead,
     * so the application can be run locally without a mail account.
     */
    private static final String USERNAME = env("MAIL_USERNAME", "");
    private static final String APP_PASSWORD = env("MAIL_APP_PASSWORD", "");
    private static final String SMTP_HOST = env("MAIL_SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT = env("MAIL_SMTP_PORT", "587");

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static boolean mailConfigured() {
        return !USERNAME.isEmpty() && !APP_PASSWORD.isEmpty();
    }

    private static void logInsteadOfSending(String to, String subject, String body) {
        System.out.println("[EmailSender] MAIL_USERNAME / MAIL_APP_PASSWORD not set - email not sent.");
        System.out.println("[EmailSender] To: " + to + " | Subject: " + subject);
        System.out.println("[EmailSender] Body:\n" + body);
    }

    public static void sendEmail(String to, String subject, String body) {
        if (!mailConfigured()) {
            logInsteadOfSending(to, subject, body);
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, APP_PASSWORD);
            }
        });

        session.setDebug(true); // Enable debug, Payara server.log can see SMTP interaction

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent to " + to);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // Send emails asynchronously to avoid blocking the interface
    public static void sendEmailAsync(String to, String subject, String body) {
        new Thread(() -> sendEmail(to, subject, body)).start();
    }

    public static void sendEmailWithAttachment(String to, String subject, String htmlBody, byte[] pdfAttachment) {
        if (!mailConfigured()) {
            logInsteadOfSending(to, subject + " (with PDF attachment)", htmlBody);
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, APP_PASSWORD);
            }
        });
        session.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // HTML content
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(htmlBody, "text/html; charset=UTF-8");

            // PDF attachment
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(pdfAttachment, "application/pdf");
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName("BookingConfirmation.pdf");

            // Merge
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("Email sent successfully to " + to);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
