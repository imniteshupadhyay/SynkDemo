package com.playmotech.api.core.response.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ErrorReportGenerator {
	// Template headers that match your Excel columns
	private static final String[] COACH_HEADERS = { "Name", "Designation", "Phone Number", "Email", "Gender", "Role",
			"Experience", "Pincode", "City", "State", "Address Line 1", "Address Line 2" };

	private static final String[] PLAYER_HEADERS = { "Full Name", "Phone", "Gender", "Date of Birth", "Email",
			"Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country" };

	private static final String[] PROGRAM_HEADERS = { "Title*", "Description", "Sport*", "Skill Level*",
			"Age Category*", "Total Max Trainees*", "Visibility*", "Registration Fee*", "Currency*", "Schedule Type*",
			"Days of Week", "Start Date* (DD-MM-YYYY)", "End Date* (DD-MM-YYYY)", "Session Start Time* (HH:MM)",
			"Session End Time* (HH:MM)", "Payment Schedule Types* (Comma-separated)",
			"Payment Amounts* (Comma-separated)" };

	private static final String[] PROGRAM_COACH_MAPPING_HEADERS = { "Program*", "Coach*", "Program ID (Hidden)",
			"Coach ID (Hidden)" };

	private static final String[] PROGRAM_PLAYER_MAPPING_HEADERS = { "Program*", "Player*", "Payment Schedule*",
			"Amount*", "Joining Date* (dd-MM-yyyy)", "Due Date (dd-MM-yyyy)" };

	private static final String[] PROGRAM_PLAYER_ENROLLMENT_HEADERS = { "Full Name", "Phone Number", "Fee Amount",
			"Payment Schedule", "Joining Date", "Next Due Date" };

	public enum TemplateType {
		COACH, PLAYER, PROGRAM, PROGRAM_COACH_MAPPING, PROGRAM_PLAYER_MAPPING, PROGRAM_PLAYER_ENROLLMENT_HEADERS
	}

	public static ByteArrayOutputStream generate(List<ErrorReportEntry> entries, TemplateType type) throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Upload Errors");

			String[] headers = getHeadersForType(type);

			// Create header row
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				headerRow.createCell(i).setCellValue(headers[i]);
			}
			// Add one more header for Errors
			headerRow.createCell(headers.length).setCellValue("Errors");

			// Fill data rows
			int rowNum = 1;
			for (ErrorReportEntry entry : entries) {
				Row row = sheet.createRow(rowNum++);
				List<String> data = entry.getOriginalData();

				// Fill original data into respective columns
				for (int i = 0; i < data.size(); i++) {
					row.createCell(i).setCellValue(data.get(i));
				}

				// Fill error messages in the last column
				row.createCell(headers.length).setCellValue(String.join("; ", entry.getErrorMessages()));
			}

			// Auto-size all columns including error column
			for (int i = 0; i <= headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out;
		}
	}

	private static String[] getHeadersForType(TemplateType type) {
		return switch (type) {
		case COACH -> COACH_HEADERS;
		case PLAYER -> PLAYER_HEADERS;
		case PROGRAM -> PROGRAM_HEADERS;
		case PROGRAM_COACH_MAPPING -> PROGRAM_COACH_MAPPING_HEADERS;
		case PROGRAM_PLAYER_MAPPING -> PROGRAM_PLAYER_MAPPING_HEADERS;
		case PROGRAM_PLAYER_ENROLLMENT_HEADERS -> PROGRAM_PLAYER_ENROLLMENT_HEADERS;
		};
	}
}