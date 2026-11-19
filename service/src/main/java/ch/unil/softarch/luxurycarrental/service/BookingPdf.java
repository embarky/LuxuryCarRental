package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Creates the PDF booking confirmation attached to the "booking received" email.
 */
public final class BookingPdf {

    private static final DeviceRgb GOLD = new DeviceRgb(201, 165, 90);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb GREY = new DeviceRgb(120, 120, 120);

    private BookingPdf() {
    }

    /**
     * Builds the PDF. Must be called inside the transaction, because it reads the
     * booking's car and customer. Returns an empty array if the PDF cannot be created.
     */
    public static byte[] create(Booking booking) {
        Car car = booking.getCar();
        Customer customer = booking.getCustomer();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(out));
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(48, 48, 48, 48);

            // Header
            doc.add(new Paragraph("LUXURY CAR RENTAL")
                    .setFontSize(18).setFont(bold).setFontColor(GOLD).setCharacterSpacing(2));
            doc.add(new Paragraph("Booking confirmation")
                    .setFontSize(24).setFont(bold).setFontColor(DARK).setMarginTop(4));
            doc.add(new Paragraph("Reference " + booking.getReference()
                    + "  ·  Issued " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .setFontSize(10).setFontColor(GREY).setMarginBottom(16));

            // Customer
            doc.add(section("Customer", bold));
            Table customerTable = table();
            addRow(bold, customerTable, "Name", customer.getFirstName() + " " + customer.getLastName());
            addRow(bold, customerTable, "Email", customer.getEmail());
            addRow(bold, customerTable, "Driving licence", nullSafe(customer.getDrivingLicenseNumber()));
            doc.add(customerTable);

            // Rental
            doc.add(section("Rental", bold));
            long days = booking.getDays();
            Table rentalTable = table();
            addRow(bold, rentalTable, "Car", EmailTemplates.carName(car));
            addRow(bold, rentalTable, "Licence plate", nullSafe(car.getLicensePlate()));
            addRow(bold, rentalTable, "Colour", nullSafe(car.getColor()));
            addRow(bold, rentalTable, "Pick-up", EmailTemplates.date(booking.getStartDate()));
            addRow(bold, rentalTable, "Return", EmailTemplates.date(booking.getEndDate()));
            addRow(bold, rentalTable, "Duration", days + (days == 1 ? " day" : " days"));
            doc.add(rentalTable);

            // Amounts
            doc.add(section("Amounts", bold));
            Table amountTable = table();
            addRow(bold, amountTable, "Daily rate", EmailTemplates.chf(car.getDailyRentalPrice()));
            addRow(bold, amountTable, "Rental total", EmailTemplates.chf(booking.getTotalCost()));
            addRow(bold, amountTable, "Deposit (refundable)", EmailTemplates.chf(booking.getDepositAmount()));
            addRow(bold, amountTable, "To pay", EmailTemplates.chf(booking.getAmountDue()));
            addRow(bold, amountTable, "Status", statusText(booking));
            doc.add(amountTable);

            doc.add(new Paragraph("Unpaid bookings are released after 24 hours. Paid bookings can be cancelled "
                    + "free of charge until 48 hours before pick-up. Please bring your driving licence when you "
                    + "pick up the car; the deposit is returned after the car has been checked on return.")
                    .setFontSize(10).setFontColor(GREY).setMarginTop(24));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            System.err.println("[BookingPdf] Could not create PDF: " + e.getMessage());
            return new byte[0];
        }
    }

    private static Paragraph section(String title, PdfFont bold) {
        return new Paragraph(title.toUpperCase())
                .setFontSize(11).setFont(bold).setFontColor(GOLD).setMarginTop(14).setMarginBottom(6);
    }

    private static Table table() {
        return new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth();
    }

    private static void addRow(PdfFont bold, Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setFontColor(GREY).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(value).setFont(bold).setFontSize(11))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(6));
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String statusText(Booking booking) {
        String status = switch (booking.getBookingStatus()) {
            case PENDING -> "Awaiting payment";
            case CONFIRMED -> "Confirmed";
            case ACTIVE -> "In progress";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Cancelled";
            case REJECTED -> "Rejected";
            case EXPIRED -> "Expired";
        };
        String payment = switch (booking.getPaymentStatus()) {
            case UNPAID -> "not paid";
            case PAID -> "paid";
            case REFUNDED -> "refunded";
        };
        return status + " (" + payment + ")";
    }
}
