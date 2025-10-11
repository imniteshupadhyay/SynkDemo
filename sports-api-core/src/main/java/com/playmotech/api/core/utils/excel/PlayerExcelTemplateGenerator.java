package com.playmotech.api.core.utils.excel;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.response.dao.UserExportDetails;

public class PlayerExcelTemplateGenerator {

	private static final String[] PLAYER_HEADERS = { "Full Name", "Phone", "Gender", "Date of Birth", "Email",
			"Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country" };

	private static final String[] PLAYER_HEADERS_WITH_ID = { "Full Name", "Phone", "Gender", "Date of Birth", "Email",
			"Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country", "ID" };

	public static Workbook generatePlayerTemplateWithData(List<UserExportDetails> existingPlayers) {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Player List");

		// Create a cell style for locked cells (name and phone only)
		CellStyle lockedCellStyle = workbook.createCellStyle();
		lockedCellStyle.setLocked(true);

		// Create a cell style for editable cells
		CellStyle editableCellStyle = workbook.createCellStyle();
		editableCellStyle.setLocked(false);

		// Create header row with ID column
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < PLAYER_HEADERS_WITH_ID.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(PLAYER_HEADERS_WITH_ID[i]);
		}

		// Populate data rows
		int rowNum = 1;
		for (UserExportDetails player : existingPlayers) {
			Row row = sheet.createRow(rowNum++);

			// Full Name - Locked
			Cell nameCell = row.createCell(0);
			nameCell.setCellValue(player.getDisplayName());
			nameCell.setCellStyle(editableCellStyle);

			// Phone - Locked
			Cell phoneCell = row.createCell(1);
			phoneCell.setCellValue(player.getPhoneNumber());
			phoneCell.setCellStyle(lockedCellStyle);

			// Gender - Editable
			Cell genderCell = row.createCell(2);
			genderCell.setCellValue(player.getGender().name());
			genderCell.setCellStyle(editableCellStyle);

			// Date of Birth - Editable
			Cell dobCell = row.createCell(3);
			dobCell.setCellValue(player.getDob());
			dobCell.setCellStyle(editableCellStyle);

			// Email - Editable
			Cell emailCell = row.createCell(4);
			emailCell.setCellValue(player.getEmailId());
			emailCell.setCellStyle(editableCellStyle);

			// Address Line 1 - Editable
			Cell addr1Cell = row.createCell(5);
			addr1Cell.setCellValue(player.getAddressLine1());
			addr1Cell.setCellStyle(editableCellStyle);

			// Address Line 2 - Editable
			Cell addr2Cell = row.createCell(6);
			addr2Cell.setCellValue(player.getAddressLine2());
			addr2Cell.setCellStyle(editableCellStyle);

			// Pincode - Editable
			Cell pincodeCell = row.createCell(7);
			pincodeCell.setCellValue(player.getPincode());
			pincodeCell.setCellStyle(editableCellStyle);

			// City - Editable
			Cell cityCell = row.createCell(8);
			cityCell.setCellValue(player.getCity());
			cityCell.setCellStyle(editableCellStyle);

			// State - Editable
			Cell stateCell = row.createCell(9);
			stateCell.setCellValue(player.getState());
			stateCell.setCellStyle(editableCellStyle);

			// Country - Editable
			Cell countryCell = row.createCell(10);
			countryCell.setCellValue(player.getCountry());
			countryCell.setCellStyle(editableCellStyle);

			// ID - Hidden and Locked (assuming UserExportDetails has an ID field)
			Cell idCell = row.createCell(11);
			idCell.setCellValue(player.getId() != null ? player.getId().toString() : ""); // Assuming getId() method
																							// exists
			idCell.setCellStyle(lockedCellStyle);
		}

		applyFieldValidationsWithData(sheet);
		autoSizeColumns(sheet, PLAYER_HEADERS_WITH_ID.length - 1); // Don't auto-size ID column

		// Hide the ID column (last column)
		sheet.setColumnHidden(11, true);

		sheet.createFreezePane(0, 1);

		// Protect the sheet to make locked cells uneditable
		sheet.protectSheet("");

		return workbook;
	}

	public static Workbook generatePlayerTemplate() {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Player List");

		// Create header row without ID column
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < PLAYER_HEADERS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(PLAYER_HEADERS[i]);
		}

		applyFieldValidations(sheet);
		autoSizeColumns(sheet, PLAYER_HEADERS.length);
		sheet.createFreezePane(0, 1);
		return workbook;
	}

	private static void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private static void applyFieldValidationsWithData(Sheet sheet) {
		// Full Name and Phone are locked, so no validation needed for editing

		// Gender dropdown (editable)
		createDropdownValidation(sheet, 1, 2, Gender.values());
		// Date of Birth - Format dd-mm-yyyy (editable)
		createDateValidation(sheet, 1, 3);
		// Email (editable)
		createEmailValidation(sheet, 1, 4);
		// Address Line 1 (editable)
		createRequiredTextValidation(sheet, 1, 5);
		// Address Line 2 (optional text, editable)
		createOptionalTextValidation(sheet, 1, 6);
		// Pincode (editable)
		createPincodeValidation(sheet, 1, 7);
		// City (editable)
		createRequiredTextValidation(sheet, 1, 8);
		// State (editable)
		createRequiredTextValidation(sheet, 1, 9);
		// Country (editable)
		createRequiredTextValidation(sheet, 1, 10);

		// No validation for ID column as it's hidden and locked
	}

	private static void applyFieldValidations(Sheet sheet) {
		// Full Name (required text)
		createRequiredTextValidation(sheet, 1, 0);
		// Phone Number
		createPhoneValidation(sheet, 1, 1);
		// Gender dropdown
		createDropdownValidation(sheet, 1, 2, Gender.values());
		// Date of Birth - Format dd-mm-yyyy
		createDateValidation(sheet, 1, 3);
		// Email
		createEmailValidation(sheet, 1, 4);
		// Address Line 1 (text)
		createRequiredTextValidation(sheet, 1, 5);
		// Address Line 2 (optional text)
		createOptionalTextValidation(sheet, 1, 6);
		// Pincode
		createPincodeValidation(sheet, 1, 7);
		// City (text)
		createRequiredTextValidation(sheet, 1, 8);
		// State (text)
		createRequiredTextValidation(sheet, 1, 9);
		// Country (text)
		createRequiredTextValidation(sheet, 1, 10);
	}

	private static void createDropdownValidation(Sheet sheet, int startRow, int col, Enum<?>[] options) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		List<String> values = new ArrayList<>();
		for (Enum<?> option : options) {
			values.add(option.name().charAt(0) + option.name().substring(1).toLowerCase());
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

	private static void createDateValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();

		// Use a custom formula to validate date format
		String formula = "AND(" + "ISNUMBER(DATEVALUE(SUBSTITUTE(SUBSTITUTE(RC,\"-\",\"/\"),\".\",\"/\"))),"
				+ "OR(SEARCH(\"??-??-????\",RC),SEARCH(\"??/??/????\",RC))" + ")";

		DataValidationConstraint constraint = helper.createCustomConstraint(formula);
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Date", "Please enter date in dd-mm-yyyy format");

		validation.setShowPromptBox(true);
		validation.createPromptBox("Date Format", "Enter date in dd-mm-yyyy format (e.g., 31-12-2000)");

		validation.setSuppressDropDownArrow(true);
		validation.setEmptyCellAllowed(true);

		sheet.addValidationData(validation);
	}

	private static void createPhoneValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(LEN(TRIM(RC))=10, ISNUMBER(VALUE(RC)))";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Phone",
				"Must be 12 digits (e.g., 919876543210)");
	}

	private static void createEmailValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "IF(ISBLANK(RC),TRUE," + "AND(" + "LEN(RC)<=254," + "ISNUMBER(FIND(\"@\", RC)),"
				+ "LEN(RC)-LEN(SUBSTITUTE(RC, \"@\", \"\"))=1," + "ISNUMBER(FIND(\".\", RC, FIND(\"@\", RC)+2)),"
				+ "LEFT(RC,1)<>\".\"," + "RIGHT(RC,1)<>\".\"))";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Email",
				"Must be valid (e.g., user@example.com)");
	}

	private static void createPincodeValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(LEN(TRIM(RC))=6, ISNUMBER(VALUE(RC)))";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Pincode",
				"Must be 6 digits (e.g., 560001)");
	}

	private static void createRequiredTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(ISTEXT(RC), LEN(TRIM(RC)) > 0)";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Required Field",
				"This field cannot be empty");
	}

	private static void createTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "ISTEXT(RC)";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Invalid Input",
				"Must be a text value");
	}

	private static void createOptionalTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "OR(ISBLANK(RC), ISTEXT(RC))";

		DataValidationConstraint constraint = helper.createCustomConstraint(formula);
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Input", "Must be a text value if provided");

		validation.setShowPromptBox(true);
		validation.createPromptBox("Optional Field", "This field is optional, but must be text if provided");

		validation.setSuppressDropDownArrow(true);
		validation.setEmptyCellAllowed(true);

		sheet.addValidationData(validation);
	}

	private static void applyValidation(Sheet sheet, DataValidationConstraint constraint, int startRow, int col,
			String title, String message) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);

		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox(title, message);

		validation.setShowPromptBox(true);
		validation.createPromptBox(title, message);

		validation.setSuppressDropDownArrow(true);
		validation.setEmptyCellAllowed(true);

		sheet.addValidationData(validation);
	}

}