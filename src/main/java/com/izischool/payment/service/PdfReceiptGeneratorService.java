package com.izischool.payment.service;

import com.izischool.payment.domain.Payment;
import com.izischool.payment.domain.Receipt;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfReceiptGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    public byte[] generateReceiptPdf(Receipt receipt) {
        Payment payment = receipt.getPayment();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 58, 138));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);

            // School Header
            String schoolName = receipt.getSchool() != null ? receipt.getSchool().getName() : "Établissement Scolaire";
            Paragraph header = new Paragraph(schoolName.toUpperCase(), titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph docType = new Paragraph("REÇU OFFICIEL DE PAIEMENT", subTitleFont);
            docType.setAlignment(Element.ALIGN_CENTER);
            docType.setSpacingAfter(20);
            document.add(docType);

            // Table with Receipt Details
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            addTableRow(table, "N° Reçu :", receipt.getReceiptNumber(), boldFont, regularFont);
            addTableRow(table, "Date d'émission :", DATE_FORMATTER.format(receipt.getIssuedAt()), boldFont, regularFont);
            addTableRow(table, "Réf. Paiement :", payment.getPaymentReference(), boldFont, regularFont);
            addTableRow(table, "Mode de Paiement :", payment.getPaymentMethod().name(), boldFont, regularFont);

            if (payment.getStudent() != null) {
                addTableRow(table, "Élève :", payment.getStudent().getFullName() + " (" + payment.getStudent().getStudentNumber() + ")", boldFont, regularFont);
            }
            if (payment.getParent() != null) {
                addTableRow(table, "Payeur / Parent :", payment.getParent().getFullName() + " (" + payment.getParent().getPhone() + ")", boldFont, regularFont);
            }

            addTableRow(table, "Montant Réglé :", String.format("%,.2f %s", receipt.getAmount(), receipt.getCurrency()), boldFont, titleFont);
            addTableRow(table, "Statut :", receipt.getStatus().name(), boldFont, regularFont);

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("Ce reçu constitue une preuve officielle de paiement enregistrée sur iziSchool.\nConservez ce document précieusement.", regularFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for receipt {}", receipt.getReceiptNumber(), e);
            throw new RuntimeException("Error generating receipt PDF: " + e.getMessage());
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setPadding(8);
        cell1.setBackgroundColor(new Color(243, 244, 246));

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valueFont));
        cell2.setPadding(8);

        table.addCell(cell1);
        table.addCell(cell2);
    }
}
