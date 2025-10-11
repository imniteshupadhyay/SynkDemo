package com.playmotech.api.core.utils.excel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.DayOfWeek;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;

public class ProgramExcelTemplateGenerator {

	private static final String[] PROGRAM_HEADERS = { "Title*", "Description", "Sport*", "Skill Level*",
			"Age Category*", "Total Max Trainees*", "Visibility*", "Registration Fee*", "Currency*", "Schedule Type*",
			"Days of Week", "Start Date* (e.g., 05-Jun-2025)", "End Date* (e.g., 15-Jul-2025)",
			"Session Start Time* (HH:MM)", "Session End Time* (HH:MM)", "Payment Schedule Types* (Comma-separated)",
			"Payment Amounts* (Comma-separated)" };

	public static Workbook generateProgramTemplate() {
		return generateProgramTemplate(List.of(Sports.values()));
	}

	public static Workbook generateProgramTemplate(List<Sports> sportsList) {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Programs");

		// Create header row
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < PROGRAM_HEADERS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(PROGRAM_HEADERS[i]);
		}

		applyFieldValidations(sheet, sportsList);
		autoSizeColumns(sheet, PROGRAM_HEADERS.length);
		sheet.createFreezePane(0, 1);
		return workbook;
	}

	private static void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private static void applyFieldValidations(Sheet sheet) {
		applyFieldValidations(sheet, List.of(Sports.values()));
	}

	private static void applyFieldValidations(Sheet sheet, List<Sports> sportsList) {
		// Title (required text)
		createRequiredTextValidation(sheet, 1, 0);

		// Description (optional text)
		createOptionalTextValidation(sheet, 1, 1);

		// Sport
		Sports[] sportsArray = sportsList.toArray(new Sports[0]);
		createDropdownValidation(sheet, 1, 2, sportsArray);

		// Skill Level
		createDropdownValidation(sheet, 1, 3, SkillLevel.values());

		// Age Category
		createDropdownValidation(sheet, 1, 4, AgeCategory.values());

		// Total Max Trainees (numeric)
		createNumericValidation(sheet, 1, 5);

		// Visibility
		createDropdownValidation(sheet, 1, 6, Visibility.values());

		// Registration Fee (numeric)
		createOptionalNumericValidation(sheet, 1, 7);

		// Currency
		createDropdownValidation(sheet, 1, 8, Currency.values());

		// Schedule Type
		createDropdownValidation(sheet, 1, 9, ScheduleType.values());

		// Days of Week (conditional based on Schedule Type)
		createDaysOfWeekValidation(sheet, 1, 10);

		// Start Date - using flexible date validation
		createFlexibleDateValidation(sheet, 1, 11);

		// End Date - using flexible date validation
		createFlexibleDateValidation(sheet, 1, 12);

		// Session Start Time
		createTimeValidation(sheet, 1, 13);

		// Session End Time
		createTimeValidation(sheet, 1, 14);

		// Payment Schedule Types
		createPaymentScheduleValidation(sheet, 1, 15);

		// Payment Amounts
		createPaymentAmountsValidation(sheet, 1, 16);
	}

	private static void createDropdownValidation(Sheet sheet, int startRow, int col, Enum<?>[] options) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		List<String> values = new ArrayList<>();
		for (Enum<?> option : options) {
			if (option instanceof AgeCategory) {
				values.add(((AgeCategory) option).toString());
			} else {
				values.add(option.name().charAt(0) + option.name().substring(1).toLowerCase());
			}
		}
		DataValidationConstraint constraint = helper.createExplicitListConstraint(values.toArray(new String[0]));
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);
		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Selection", "Choose from dropdown list");
		validation.setShowPromptBox(true);
		validation.createPromptBox("Selection Required", "Please select a value from the dropdown");
		sheet.addValidationData(validation);
	}

	private static void createRequiredTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(ISTEXT(RC), LEN(TRIM(RC)) > 0)";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Required Field",
				"This field cannot be empty");
	}

	private static void createOptionalTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "OR(ISBLANK(RC), ISTEXT(RC))";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Optional Field",
				"Must be a text value if provided");
	}

	private static void createNumericValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		DataValidationConstraint constraint = helper.createNumericConstraint(
				DataValidationConstraint.ValidationType.DECIMAL, DataValidationConstraint.OperatorType.GREATER_THAN,
				"0", null);

		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Number", "Must be a positive number");

		validation.setShowPromptBox(true);
		validation.createPromptBox("Numeric Value", "Enter a positive number");

		sheet.addValidationData(validation);
	}

	private static void createOptionalNumericValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		DataValidationConstraint constraint = helper.createNumericConstraint(
				DataValidationConstraint.ValidationType.DECIMAL, DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
				"0", null); // Allow 0 and positive numbers

		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Number", "Must be 0 or a positive number");

		validation.setShowPromptBox(true);
		validation.createPromptBox("Numeric Value", "Enter 0 or a positive number");

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

	private static void createTimeValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(ISTEXT(RC), ISNUMBER(TIMEVALUE(RC)))";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Time",
				"Enter time in HH:MM format");
	}

	private static void createDaysOfWeekValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String[] days = getEnumValues(DayOfWeek.values());
		String formula = "OR(ISBLANK(RC), " + "AND(ISTEXT(RC), "
				+ "OR(INDIRECT(\"J\" & ROW())=\"Custom\", INDIRECT(\"J\" & ROW())=\"Weekdays\", INDIRECT(\"J\" & ROW())=\"Weekends\"), "
				+ "SUMPRODUCT(--ISNUMBER(SEARCH(\",\"&TRIM(MID(SUBSTITUTE(RC,\",\",REPT(\" \",99)),(ROW(INDIRECT(\"1:\"&LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))+1))-1)*99+1,99))&\",\",\",\"&\""
				+ String.join(",", days) + "\"&\",\")))=" + "(LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))+1)))";

		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Days of Week",
				"Enter comma-separated days (e.g., Monday,Wednesday,Friday) when Schedule Type is Custom, Weekdays, or Weekends");
	}

	private static void createPaymentScheduleValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String[] options = getEnumValues(PaymentSchedule.values());
		String allowedTypesExample = String.join(", ", options); // E.g., "FULL, MONTHLY, QUARTERLY"
		String formula = "AND(NOT(ISBLANK(RC)), "
				+ "SUMPRODUCT(--ISNUMBER(SEARCH(\",\"&TRIM(MID(SUBSTITUTE(RC,\",\",REPT(\" \",99)),(ROW(INDIRECT(\"1:\"&LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))+1))-1)*99+1,99))&\",\",\",\"&\""
				+ String.join(",", options) + "\"&\",\")))=" + "(LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))+1))";

		applyValidation(sheet,
				helper.createCustomConstraint(formula),
				startRow,
				col,
				"Payment Schedule Types",
				"Enter comma-separated payment types (e.g., " + allowedTypesExample + ")");

	}

	private static void createPaymentAmountsValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(NOT(ISBLANK(RC)), " + "LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))="
				+ "LEN(INDIRECT(\"P\" & ROW()))-LEN(SUBSTITUTE(INDIRECT(\"P\" & ROW()),\",\",\"\")), "
				+ "SUMPRODUCT(--ISNUMBER(VALUE(TRIM(MID(SUBSTITUTE(RC,\",\",REPT(\" \",99)),(ROW(INDIRECT(\"1:\"&LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))+1))-1)*99+1,99)))))="
				+ "(LEN(RC)-LEN(SUBSTITUTE(RC,\",\",\"\"))+1))";

		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Payment Amounts",
				"Enter numeric amounts matching the number of payment types");
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

		if (constraint.getValidationType() == DataValidationConstraint.ValidationType.LIST) {
			validation.setSuppressDropDownArrow(false);
		} else {
			validation.setSuppressDropDownArrow(true);
		}

		validation.setEmptyCellAllowed(true);
		sheet.addValidationData(validation);
	}

	private static String[] getEnumValues(Enum<?>[] values) {
		return Arrays.stream(values).map(e -> e.name().charAt(0) + e.name().substring(1).toLowerCase())
				.toArray(String[]::new);
	}
}