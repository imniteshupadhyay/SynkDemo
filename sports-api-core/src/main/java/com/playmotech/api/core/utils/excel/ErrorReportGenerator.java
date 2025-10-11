package com.playmotech.api.core.utils.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.playmotech.api.core.response.excel.ErrorReportEntry;

public class ErrorReportGenerator {

	public static ByteArrayOutputStream generate(String[] headers, List<ErrorReportEntry> entries) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Upload Errors");

			int rowNum = 0;
			Row headerRow = sheet.createRow(rowNum++);
			int colNum = 0;

			// Add original template headers
			for (String header : headers) {
				Cell cell = headerRow.createCell(colNum++);
				cell.setCellValue(header);
			}

			// Add extra header for Error Messages
			Cell errorCell = headerRow.createCell(colNum);
			errorCell.setCellValue("Errors");

			// Populate data
			for (ErrorReportEntry entry : entries) {
				Row row = sheet.createRow(rowNum++);
				List<String> data = entry.getOriginalData();

				// Write user data
				for (int i = 0; i < headers.length; i++) {
					Cell cell = row.createCell(i);
					cell.setCellValue(i < data.size() ? data.get(i) : "");
				}

				// Write error messages
				Cell error = row.createCell(headers.length);
				error.setCellValue(String.join("; ", entry.getErrorMessages()));
			}

			// Autosize columns
			for (int i = 0; i <= headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out;
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate error report", e);
		}
	}
}
