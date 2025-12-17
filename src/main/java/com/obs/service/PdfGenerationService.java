package com.obs.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.obs.entity.Account;
import com.obs.entity.Transaction;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    public byte[] generateTransactionInvoice(Transaction transaction) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            String currencySymbol = "Rs. ";
            try {
                PdfFont font = PdfFontFactory.createFont("C:/Windows/Fonts/arial.ttf", PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                document.setFont(font);
                currencySymbol = "\u20B9";
            } catch (Exception e) {
                 System.err.println("Could not load font for Rupee symbol: " + e.getMessage());
            }

            document.add(new Paragraph("Online Banking System")
                    .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Transaction Invoice")
                    .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            document.add(new Paragraph("Transaction ID: " + transaction.getId()));
            document.add(new Paragraph("Date: " + transaction.getTimestamp().format(formatter)));
            document.add(new Paragraph("Transaction Type: " + transaction.getType()));
            document.add(new Paragraph("Amount: " + currencySymbol + transaction.getAmount()));
            document.add(new Paragraph("Description: " + (transaction.getDescription() != null ? transaction.getDescription() : "N/A")));
            
            if (transaction.getAccount() != null) {
                document.add(new Paragraph("Account Number: " + transaction.getAccount().getAccountNumber()));
            }
            if (transaction.getTargetAccountNumber() != null) {
                 document.add(new Paragraph("Target Account: " + transaction.getTargetAccountNumber()));
            }

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Status: " + (transaction.getStatus() != null ? transaction.getStatus() : "COMPLETED")));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF invoice");
        }
        return baos.toByteArray();
    }

    public byte[] generateAccountStatement(Account account, java.util.List<Transaction> transactions) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            String currencySymbol = "Rs. ";
            try {
                PdfFont font = PdfFontFactory.createFont("C:/Windows/Fonts/arial.ttf", PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                document.setFont(font);
                currencySymbol = "\u20B9";
            } catch (Exception e) {
                 System.err.println("Could not load font for Rupee symbol: " + e.getMessage());
            }

            document.add(new Paragraph("Online Banking System")
                    .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Account Statement")
                    .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Account Number: " + account.getAccountNumber()));
            document.add(new Paragraph("Account Type: " + account.getAccountType()));
            document.add(new Paragraph("Current Balance: " + currencySymbol + account.getBalance()));
            document.add(new Paragraph("User: " + account.getUser().getUsername()));
            document.add(new Paragraph("\n"));

            float[] columnWidths = {1, 3, 2, 2, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Cell().add(new Paragraph("ID").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Type").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Status").setBold()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (Transaction t : transactions) {
                table.addCell(t.getId().toString());
                table.addCell(t.getTimestamp().format(formatter));
                table.addCell(t.getType());
                table.addCell(currencySymbol + t.getAmount().toString());
                table.addCell(t.getStatus() != null ? t.getStatus() : "N/A");
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating statement PDF");
        }
        return baos.toByteArray();
    }
}
