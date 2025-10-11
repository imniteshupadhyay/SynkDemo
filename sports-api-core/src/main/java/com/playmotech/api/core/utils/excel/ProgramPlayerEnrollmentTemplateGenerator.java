package com.playmotech.api.core.utils.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.playmotech.api.core.constants.PaymentSchedule;

public class ProgramPlayerEnrollmentTemplateGenerator {

	private static final String[] PROGRAM_PLAYER_ENROLLMENT_HEADERS = { "Full Name", "Phone Number", "Fee Amount",
			"Payment Schedule", "Joining Date (e.g., 05-Jun-2025)", "Next Due Date (e.g., 15-Jul-2025)" };

	// Get Payment Schedule options directly from enum
	private static String[] getPaymentScheduleOptions() {
		PaymentSchedule[] schedules = PaymentSchedule.values();
		String[] options = new String[schedules.length];
		for (int i = 0; i < schedules.length; i++) {
			options[i] = schedules[i].name();
		}
		return options;
	}

	public static Workbook generateProgramPlayerEnrollmentTemplate() {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Program Player Enrollment");

		// Create header row
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < PROGRAM_PLAYER_ENROLLMENT_HEADERS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(PROGRAM_PLAYER_ENROLLMENT_HEADERS[i]);
		}

		applyFieldValidations(sheet);
		autoSizeColumns(sheet, PROGRAM_PLAYER_ENROLLMENT_HEADERS.length);
		sheet.createFreezePane(0, 1);
		return workbook;
	}

	private static void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private static void applyFieldValidations(Sheet sheet) {
		// Full Name (required text)
		createRequiredTextValidation(sheet, 1, 0);

		// Phone Number (required, 10 digits)
		createPhoneValidation(sheet, 1, 1);

		// Fee Amount (required, non-negative number)
		createFeeAmountValidation(sheet, 1, 2);

		// Payment Schedule (required dropdown)
		createPaymentScheduleValidation(sheet, 1, 3);

		// Joining Date (optional, MMM/MMMM with YYYY format)
		createFlexibleDateValidation(sheet, 1, 4);

		// Next Due Date (optional, MMM/MMMM with YYYY format)
		createFlexibleDateValidation(sheet, 1, 5);
	}

	private static void createRequiredTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(ISTEXT(RC), LEN(TRIM(RC)) > 0)";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Required Field",
				"This field cannot be empty and must be text");
	}

	private static void createPhoneValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		// Allow phone numbers with or without special characters, but must result in 10
		// digits
		String formula = "AND(LEN(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(RC,\" \",\"\"),\"-\",\"\"),\"(\",\"\"),\")\",\"\"),\"+\",\"\"))=10, "
				+ "ISNUMBER(VALUE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(RC,\" \",\"\"),\"-\",\"\"),\"(\",\"\"),\")\",\"\"),\"+\",\"\"))))";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Phone Number",
				"Must be exactly 12 digits (e.g., 919876543210)");
	}

	private static void createFeeAmountValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(ISNUMBER(VALUE(RC)), VALUE(RC)>=0)";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Fee Amount",
				"Must be a non-negative number (e.g., 1000)");
	}

	private static void createPaymentScheduleValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String[] paymentScheduleOptions = getPaymentScheduleOptions();
		DataValidationConstraint constraint = helper.createExplicitListConstraint(paymentScheduleOptions);

		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Payment Schedule",
				"Please select a valid payment schedule from the dropdown list");

		validation.setShowPromptBox(true);
		validation.createPromptBox("Payment Schedule",
				"Select payment schedule: " + String.join(", ", paymentScheduleOptions));

		validation.setSuppressDropDownArrow(false); // Show dropdown arrow
		validation.setEmptyCellAllowed(false); // Make it required

		sheet.addValidationData(validation);
	}

	private static void createFlexibleDateValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();

		// Create a formula that validates only the specific formats from your
		// formatters list
		// Supports: dd-MMM-yyyy, d-MMM-yyyy, dd/MMM/yyyy, d/MMM/yyyy, dd MMM yyyy, d
		// MMM yyyy
		// dd-MMMM-yyyy, d-MMMM-yyyy, dd/MMMM/yyyy, d/MMMM/yyyy, dd MMMM yyyy, d MMMM
		// yyyy
		String formula = "IF(ISBLANK(RC), TRUE, " + "AND(" + "LEN(RC)>=9, " + // Minimum length (d-MMM-yyyy = 9 chars)
				"LEN(RC)<=20, " + // Maximum length (dd-MMMM-yyyy = 15 chars, with buffer)
				"OR(" +
				// Test for dd-MMM-yyyy and d-MMM-yyyy patterns
				"AND(" + "LEN(SUBSTITUTE(SUBSTITUTE(RC,\"-\",\"\"),\"/\",\"\"))=LEN(RC)-2, " + // Exactly 2 separators
				"OR(" + "AND(ISNUMBER(LEFT(RC,2)*1), MID(RC,3,1)=\"-\", ISNUMBER(RIGHT(RC,4)*1)), " + // dd-MMM-yyyy
				"AND(ISNUMBER(LEFT(RC,1)*1), MID(RC,2,1)=\"-\", ISNUMBER(RIGHT(RC,4)*1)), " + // d-MMM-yyyy
				"AND(ISNUMBER(LEFT(RC,2)*1), MID(RC,3,1)=\"/\", ISNUMBER(RIGHT(RC,4)*1)), " + // dd/MMM/yyyy
				"AND(ISNUMBER(LEFT(RC,1)*1), MID(RC,2,1)=\"/\", ISNUMBER(RIGHT(RC,4)*1))" + // d/MMM/yyyy
				"), " + "ISNUMBER(DATEVALUE(RC))" + // Can be parsed as valid date
				"), " +
				// Test for dd MMM yyyy and d MMM yyyy patterns (space separated)
				"AND(" + "LEN(SUBSTITUTE(RC,\" \",\"\"))=LEN(RC)-2, " + // Exactly 2 spaces
				"OR(" + "AND(ISNUMBER(LEFT(RC,2)*1), MID(RC,3,1)=\" \", ISNUMBER(RIGHT(RC,4)*1)), " + // dd MMM yyyy
				"AND(ISNUMBER(LEFT(RC,1)*1), MID(RC,2,1)=\" \", ISNUMBER(RIGHT(RC,4)*1))" + // d MMM yyyy
				"), " + "ISNUMBER(DATEVALUE(RC))" + // Can be parsed as valid date
				"), " +
				// Test for dd-MMMM-yyyy and d-MMMM-yyyy patterns (longer month names)
				"AND(" + "LEN(RC)>=12, " + // Minimum for full month name format
				"OR(" + "AND(ISNUMBER(LEFT(RC,2)*1), MID(RC,3,1)=\"-\", ISNUMBER(RIGHT(RC,4)*1)), " + // dd-MMMM-yyyy
				"AND(ISNUMBER(LEFT(RC,1)*1), MID(RC,2,1)=\"-\", ISNUMBER(RIGHT(RC,4)*1)), " + // d-MMMM-yyyy
				"AND(ISNUMBER(LEFT(RC,2)*1), MID(RC,3,1)=\"/\", ISNUMBER(RIGHT(RC,4)*1)), " + // dd/MMMM/yyyy
				"AND(ISNUMBER(LEFT(RC,1)*1), MID(RC,2,1)=\"/\", ISNUMBER(RIGHT(RC,4)*1)), " + // d/MMMM/yyyy
				"AND(ISNUMBER(LEFT(RC,2)*1), MID(RC,3,1)=\" \", ISNUMBER(RIGHT(RC,4)*1)), " + // dd MMMM yyyy
				"AND(ISNUMBER(LEFT(RC,1)*1), MID(RC,2,1)=\" \", ISNUMBER(RIGHT(RC,4)*1))" + // d MMMM yyyy
				"), " + "ISNUMBER(DATEVALUE(RC))" + // Can be parsed as valid date
				")" + "), " +
				// Ensure 4-digit year (1900-2100)
				"AND(ISNUMBER(RIGHT(RC,4)*1), RIGHT(RC,4)*1>=1900, RIGHT(RC,4)*1<=2100)" + ")" + ")";

		DataValidationConstraint constraint = helper.createCustomConstraint(formula);
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Date Format",
				"Use: dd-MMM-yyyy, d-MMM-yyyy, dd/MMM/yyyy, d/MMM/yyyy, dd MMM yyyy, d MMM yyyy, "
						+ "dd-MMMM-yyyy, d-MMMM-yyyy, dd/MMMM/yyyy, d/MMMM/yyyy, dd MMMM yyyy, d MMMM yyyy");

		validation.setShowPromptBox(true);
		validation.createPromptBox("Date Format",
				"Enter date with month names and 4-digit year. Examples: 31-Jan-2024, 1-Jan-2024, "
						+ "31/Jan/2024, 1/Jan/2024, 31 Jan 2024, 1 Jan 2024, 15-December-2023, 1-December-2023, "
						+ "15/December/2023, 1/December/2023, 15 December 2023, 1 December 2023. Optional field.");

		validation.setSuppressDropDownArrow(true);
		validation.setEmptyCellAllowed(true);
		sheet.addValidationData(validation);
	}

	private static void applyValidation(Sheet sheet, DataValidationConstraint constraint, int startRow, int col,
			String title, String message) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.WARNING);
		validation.setShowErrorBox(true);
		validation.createErrorBox(title, message);

		validation.setShowPromptBox(true);
		validation.createPromptBox(title, message);

		validation.setSuppressDropDownArrow(true);
		validation.setEmptyCellAllowed(true);

		sheet.addValidationData(validation);
	}
}