package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.domain.entities.Admin;
import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Builds the HTML emails sent by the application.
 * <p>
 * Every email shares one layout (header, content card, footer) with inline styles, because
 * many email clients ignore style sheets. Values that come from users (names, reasons)
 * are HTML-escaped. Templates are built synchronously, inside the transaction, so lazily
 * loaded entity fields can be read safely; only the sending happens in the background.
 * </p>
 */
public final class EmailTemplates {

    /** Subject and HTML body of an email. */
    public record Email(String subject, String html) {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH);
    private static final String GOLD = "#C9A55A";

    private EmailTemplates() {
    }

    // -------------------------------------------------------------------------
    // Accounts
    // -------------------------------------------------------------------------

    public static Email customerWelcome(Customer customer) {
        return new Email("Welcome to Luxury Car Rental",
                layout("Welcome, " + esc(customer.getFirstName()) + "!",
                        paragraph("Your account has been created. You can now browse our fleet and book "
                                + "your next car in a few clicks.")
                                + details(
                                row("Name", esc(customer.getFirstName() + " " + customer.getLastName())),
                                row("Email", esc(customer.getEmail())))
                                + paragraph("We look forward to having you on the road with us.")));
    }

    public static Email adminWelcome(Admin admin) {
        return new Email("Your administrator account",
                layout("Welcome to the admin team",
                        paragraph("Hi " + esc(admin.getName()) + ", an administrator account has been "
                                + "created for you.")
                                + details(
                                row("Username", esc(admin.getUsername())),
                                row("Email", esc(admin.getEmail())))
                                + paragraph("Keep your password secure and never share it.")));
    }

    public static Email passwordResetCode(String name, String code, int validMinutes) {
        return new Email("Your password reset code",
                layout("Reset your password",
                        paragraph("Hi " + esc(name) + ", use this code to choose a new password:")
                                + "<div style=\"margin:24px 0;text-align:center;\">"
                                + "<span style=\"display:inline-block;padding:14px 28px;font-size:30px;"
                                + "letter-spacing:8px;font-weight:bold;background:#f6f1e6;border:1px solid "
                                + GOLD + ";border-radius:8px;color:#1a1a1a;\">" + esc(code) + "</span></div>"
                                + paragraph("The code expires in " + validMinutes + " minutes. If you did not "
                                + "ask for a new password, you can ignore this email.")));
    }

    public static Email passwordChanged(String name) {
        return new Email("Your password has been changed",
                layout("Password changed",
                        paragraph("Hi " + esc(name) + ", the password of your account has just been changed.")
                                + paragraph("If this was not you, please contact our team immediately.")));
    }

    // -------------------------------------------------------------------------
    // Bookings
    // -------------------------------------------------------------------------

    public static Email bookingCreated(Booking booking) {
        return new Email("Booking received – " + carName(booking.getCar()),
                layout("We have received your booking",
                        paragraph("Hi " + esc(booking.getCustomer().getFirstName()) + ", thank you for "
                                + "choosing us. Your booking is <b>pending</b>: pay it from your bookings "
                                + "page, or our team will confirm it shortly.")
                                + bookingDetails(booking)
                                + paragraph("Your booking confirmation is attached as a PDF.")));
    }

    public static Email bookingPaid(Booking booking, double amountPaid) {
        return new Email("Payment received – booking confirmed",
                layout("Your booking is confirmed",
                        paragraph("Hi " + esc(booking.getCustomer().getFirstName()) + ", we have received "
                                + "your payment of <b>" + chf(amountPaid) + "</b> (rental and deposit).")
                                + bookingDetails(booking)
                                + paragraph("The deposit is returned after the car is brought back.")));
    }

    public static Email bookingConfirmed(Booking booking, double depositCharged) {
        return new Email("Booking confirmed – " + carName(booking.getCar()),
                layout("Your booking is confirmed",
                        paragraph("Hi " + esc(booking.getCustomer().getFirstName()) + ", good news: our "
                                + "team has confirmed your booking. A deposit of <b>" + chf(depositCharged)
                                + "</b> has been taken from your account balance.")
                                + bookingDetails(booking)
                                + paragraph("Please bring your driving licence when you pick up the car.")));
    }

    public static Email bookingCompleted(Booking booking, double amountCharged) {
        String charge = amountCharged > 0
                ? "The remaining amount of <b>" + chf(amountCharged) + "</b> has been taken from your balance."
                : "No further payment is due.";
        return new Email("Thank you for driving with us",
                layout("Your rental is complete",
                        paragraph("Hi " + esc(booking.getCustomer().getFirstName()) + ", thank you for "
                                + "returning the " + esc(carName(booking.getCar())) + ". " + charge)
                                + bookingDetails(booking)
                                + paragraph("We hope to see you again soon.")));
    }

    public static Email bookingCancelled(Booking booking, double refund) {
        return new Email("Booking cancelled",
                layout("Your booking has been cancelled",
                        paragraph("Hi " + esc(booking.getCustomer().getFirstName()) + ", your booking has "
                                + "been cancelled."
                                + (refund > 0 ? " <b>" + chf(refund) + "</b> has been returned to your balance." : ""))
                                + bookingDetails(booking)));
    }

    public static Email bookingRejected(Booking booking, String reason, double refund) {
        return new Email("Your booking could not be accepted",
                layout("Booking not accepted",
                        paragraph("Hi " + esc(booking.getCustomer().getFirstName()) + ", unfortunately we "
                                + "cannot accept your booking.")
                                + (reason == null || reason.isBlank() ? ""
                                : details(row("Reason", esc(reason))))
                                + (refund > 0 ? paragraph("<b>" + chf(refund) + "</b> has been returned to "
                                + "your balance.") : "")
                                + bookingDetails(booking)
                                + paragraph("Feel free to choose another car or other dates.")));
    }

    // -------------------------------------------------------------------------
    // Shared helpers (also used for the PDF confirmation)
    // -------------------------------------------------------------------------

    /** Short booking reference shown to customers, e.g. "A1B2C3D4". */
    public static String reference(Booking booking) {
        return booking.getBookingId() == null ? "-"
                : booking.getBookingId().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    public static String carName(Car car) {
        return car.getCarType().getBrand() + " " + car.getCarType().getModel();
    }

    public static long rentalDays(Booking booking) {
        return ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate()) + 1;
    }

    public static String date(java.time.LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    public static String chf(double amount) {
        return String.format(Locale.ROOT, "CHF %,.2f", amount).replace(',', '\'');
    }

    private static String bookingDetails(Booking booking) {
        Car car = booking.getCar();
        return details(
                row("Reference", reference(booking)),
                row("Car", esc(carName(car))),
                row("Licence plate", esc(car.getLicensePlate())),
                row("Pick-up", date(booking.getStartDate())),
                row("Return", date(booking.getEndDate())),
                row("Duration", rentalDays(booking) + (rentalDays(booking) == 1 ? " day" : " days")),
                row("Rental", chf(booking.getTotalCost())),
                row("Deposit", chf(booking.getDepositAmount())),
                row("Status", String.valueOf(booking.getBookingStatus())));
    }

    private static String layout(String heading, String content) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f2f2f2;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f2f2f2;padding:32px 0;font-family:Arial,Helvetica,sans-serif;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:600px;width:100%;background:#ffffff;border-radius:10px;overflow:hidden;\">"
                // Header
                + "<tr><td style=\"background:#1a1a1a;padding:24px 32px;border-bottom:3px solid " + GOLD + ";\">"
                + "<span style=\"color:" + GOLD + ";font-size:20px;font-weight:bold;letter-spacing:3px;\">"
                + "LUXURY CAR RENTAL</span></td></tr>"
                // Content
                + "<tr><td style=\"padding:32px;color:#1a1a1a;font-size:15px;line-height:1.6;\">"
                + "<h1 style=\"margin:0 0 16px;font-size:22px;color:#1a1a1a;\">" + heading + "</h1>"
                + content
                + "</td></tr>"
                // Footer
                + "<tr><td style=\"background:#fafafa;padding:20px 32px;color:#888;font-size:12px;"
                + "border-top:1px solid #eee;\">Luxury Car Rental · Lausanne, Switzerland<br/>"
                + "This is an automated message from a demo application.</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private static String paragraph(String html) {
        return "<p style=\"margin:0 0 16px;\">" + html + "</p>";
    }

    private static String details(String... rows) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin:8px 0 20px;border:1px solid #eee;border-radius:8px;font-size:14px;\">"
                + String.join("", rows) + "</table>";
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:10px 16px;color:#777;width:40%;border-bottom:1px solid #f0f0f0;\">"
                + label + "</td><td style=\"padding:10px 16px;font-weight:bold;border-bottom:1px solid #f0f0f0;\">"
                + value + "</td></tr>";
    }

    /** Escapes text for safe use inside HTML. */
    static String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
