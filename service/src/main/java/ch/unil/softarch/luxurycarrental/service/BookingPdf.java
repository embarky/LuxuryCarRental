package ch.unil.softarch.luxurycarrental.service;

import ch.unil.softarch.luxurycarrental.domain.entities.Booking;
import ch.unil.softarch.luxurycarrental.domain.entities.Car;
import ch.unil.softarch.luxurycarrental.domain.entities.Customer;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
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
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(48, 48, 48, 48);

            // Header
            doc.add(new Paragraph("LUXURY CAR RENTAL")
                    .setFontSize(18).setBold().setFontColor(GOLD).setCharacterSpacing(2));
            doc.add(new Paragraph("Booking confirmation")
                    .setFontSize(24).setBold().setFontColor(DARK).setMarginTop(4));
            doc.add(new Paragraph("Reference " + EmailTemplates.reference(booking)
                    + "  ·  Issued " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .setFontSize(10).setFontColor(GREY).setMarginBottom(16));

            // Customer
            doc.add(section("Customer"));
            Table customerTable = table();
            addRow(customerTable, "Name", customer.getFirstName() + " " + customer.getLastName());
            addRow(customerTable, "Email", customer.getEmail());
            addRow(customerTable, "Driving licence", nullSafe(customer.getDrivingLicenseNumber()));
            doc.add(customerTable);

            // Rental
            doc.add(section("Rental"));
            long days = EmailTemplates.rentalDays(booking);
            Table rentalTable = table();
            addRow(rentalTable, "Car", EmailTemplates.carName(car));
            addRow(rentalTable, "Licence plate", nullSafe(car.getLicensePlate()));
            addRow(rentalTable, "Colour", nullSafe(car.getColor()));
            addRow(rentalTable, "Pick-up", EmailTemplates.date(booking.getStartDate()));
            addRow(rentalTable, "Return", EmailTemplates.date(booking.getEndDate()));
            addRow(rentalTable, "Duration", days + (days == 1 ? " day" : " days"));
            doc.add(rentalTable);

            // Amounts
            doc.add(section("Amounts"));
            Table amountTable = table();
            addRow(amountTable, "Daily rate", EmailTemplates.chf(car.getDailyRentalPrice()));
            addRow(amountTable, "Rental total", EmailTemplates.chf(booking.getTotalCost()));
            addRow(amountTable, "Deposit (refundable)", EmailTemplates.chf(booking.getDepositAmount()));
            addRow(amountTable, "Status", booking.getBookingStatus() + " / payment " + booking.getPaymentStatus());
            doc.add(amountTable);

            doc.add(new Paragraph("Please bring your driving licence and this confirmation when you pick up "
                    + "the car. The deposit is returned after the car has been checked on return.")
                    .setFontSize(10).setFontColor(GREY).setMarginTop(24));

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            System.err.println("[BookingPdf] Could not create PDF: " + e.getMessage());
            return new byte[0];
        }
    }

    private static Paragraph section(String title) {
        return new Paragraph(title.toUpperCase())
                .setFontSize(11).setBold().setFontColor(GOLD).setMarginTop(14).setMarginBottom(6);
    }

    private static Table table() {
        return new Table(UnitValue.createPercentArray(new float[]{35, 65})).useAllAvailableWidth();
    }

    private static void addRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setFontColor(GREY).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(6));
        table.addCell(new Cell().add(new Paragraph(value).setBold().setFontSize(11))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)).setPadding(6));
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
