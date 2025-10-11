package com.playmotech.api.core.services.impl;// PdfService.java

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.playmotech.api.core.dto.ReceiptDto;

@Service
public class PdfService {

	public byte[] generateReceiptPdf(ReceiptDto receipt) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument pdf = new PdfDocument(writer);
		Document document = new Document(pdf);

		// Header with smaller logo
//        Image logo = new Image(ImageDataFactory.create(
//                new ClassPathResource(receipt.getCustomerLogoPath()).getURL()))
//                .setWidth(50)  // Reduced from 100 to 50
//                .setAutoScaleHeight(true);

		Div header = new Div().setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(10).setMargins(0, 0, 0, 0);

//        header.add(logo);
		header.add(new Paragraph(receipt.getMyCompanyName()).setFontSize(16) // Slightly reduced from 18
				.setBold().setMarginLeft(10).setRelativePosition(10, 0, 0, 0)); // Adjusted position for smaller logo

		document.add(header);

		// Receipt Details
		Table infoTable = new Table(2).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);

		infoTable.addCell(
				new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("Bill To: " + receipt.getCustomerName())));
		infoTable.addCell(new Cell().setBorder(Border.NO_BORDER)
				.add(new Paragraph("Receipt No: REC-" + receipt.getReceiptId()).setTextAlignment(TextAlignment.RIGHT)));
		infoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("Date: "
				+ receipt.getTransactionDateTime().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")))));
		infoTable.addCell(
				new Cell().setBorder(Border.NO_BORDER).add(new Paragraph(" ").setTextAlignment(TextAlignment.RIGHT)));

		document.add(infoTable);

		// Amount Details Table
		float[] columnWidths = { 4, 1 };
		Table itemsTable = new Table(UnitValue.createPercentArray(columnWidths))
				.setWidth(UnitValue.createPercentValue(100));

		// Table Header
		itemsTable.addHeaderCell(new Cell().setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE)
				.add(new Paragraph("Description").setBold()));
		itemsTable.addHeaderCell(new Cell().setBackgroundColor(ColorConstants.GRAY).setFontColor(ColorConstants.WHITE)
				.add(new Paragraph("Amount").setBold()));

		// Table Rows
		boolean alternate = true;
		for (ReceiptDto.TransactionItem item : receipt.getItems()) {
			itemsTable.addCell(
					new Cell().setBackgroundColor(alternate ? ColorConstants.WHITE : new DeviceRgb(245, 245, 245))
							.add(new Paragraph(item.getDescription())));
			itemsTable.addCell(
					new Cell().setBackgroundColor(alternate ? ColorConstants.WHITE : new DeviceRgb(245, 245, 245))
							.add(new Paragraph(String.format("INR %.2f", item.getAmount()))));
			alternate = !alternate;
		}

		document.add(itemsTable);

		// Total
		Table totalTable = new Table(2).setWidth(UnitValue.createPercentValue(50)).setMarginTop(20)
				.setHorizontalAlignment(HorizontalAlignment.RIGHT);

		totalTable.addCell(new Cell().setBorder(Border.NO_BORDER)
				.add(new Paragraph("Total").setBold().setTextAlignment(TextAlignment.RIGHT)));
		totalTable.addCell(new Cell().setBorder(Border.NO_BORDER)
				.add(new Paragraph(String.format("INR %.2f", receipt.getTotalAmount())).setBold()
						.setTextAlignment(TextAlignment.RIGHT)));

		document.add(totalTable);

		// Footer
		Div footer = new Div().setBackgroundColor(ColorConstants.LIGHT_GRAY).setPadding(10).setMargins(20, 0, 0, 0)
				.setTextAlignment(TextAlignment.CENTER);

		footer.add(new Paragraph("Payment received by " + receipt.getMyCompanyName()).setFontSize(10));
		document.add(footer);

		document.close();
		return baos.toByteArray();
	}
}