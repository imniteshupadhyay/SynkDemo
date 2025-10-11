package com.playmotech.api.core.utils.excel;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.response.dao.UserExportDetails;

public class CoachExcelTemplateGenerator {

	private static final String[] COACH_TEMPLATE_HEADERS = { "Name", "Phone Number", "Designation", "Email", "Gender",
			"Role", "Experience (Months)", "Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country" };

	private static final String[] COACH_TEMPLATE_HEADERS_WITH_ID = { "Name", "Phone Number", "Designation", "Email",
			"Gender", "Role", "Experience (Months)", "Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country",
			"ID" };

	public static Workbook generateCoachTemplateWithData(List<UserExportDetails> existingCoaches) {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Coach List");

		// Create locked cell style for Name and Phone only
		CellStyle lockedCellStyle = workbook.createCellStyle();
		lockedCellStyle.setLocked(true);

		// Create unlocked cell style for all other editable fields
		CellStyle editableCellStyle = workbook.createCellStyle();
		editableCellStyle.setLocked(false);

		// Create header row with ID column
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < COACH_TEMPLATE_HEADERS_WITH_ID.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(COACH_TEMPLATE_HEADERS_WITH_ID[i]);
		}

		int rowNum = 1;
		for (UserExportDetails coach : existingCoaches) {
			Row row = sheet.createRow(rowNum++);

			// Name (column 0) - Locked
			Cell nameCell = row.createCell(0);
			nameCell.setCellValue(coach.getDisplayName());
			nameCell.setCellStyle(editableCellStyle);

			// Phone Number (column 1) - Locked
			Cell phoneCell = row.createCell(1);
			phoneCell.setCellValue(coach.getPhoneNumber());
			phoneCell.setCellStyle(lockedCellStyle);

			// Designation (column 2) - Editable
			Cell designationCell = row.createCell(2);
			designationCell.setCellValue(coach.getDesignation());
			designationCell.setCellStyle(editableCellStyle);

			// Email (column 3) - Editable
			Cell emailCell = row.createCell(3);
			emailCell.setCellValue(coach.getEmailId());
			emailCell.setCellStyle(editableCellStyle);

			// Gender (column 4) - Editable
			Cell genderCell = row.createCell(4);
			genderCell.setCellValue(coach.getGender().name());
			genderCell.setCellStyle(editableCellStyle);

			// Role (column 5) - Editable
			Cell roleCell = row.createCell(5);
			roleCell.setCellValue(coach.getRole().name());
			roleCell.setCellStyle(editableCellStyle);

			// Experience (column 6) - Editable
			Cell experienceCell = row.createCell(6);
			if (coach.getExperienceInMonths() != null) {
				experienceCell.setCellValue(coach.getExperienceInMonths());
			} else {
				experienceCell.setBlank();
			}
			experienceCell.setCellStyle(editableCellStyle);

			// Address Line 1 (column 7) - Editable
			Cell address1Cell = row.createCell(7);
			address1Cell.setCellValue(coach.getAddressLine1());
			address1Cell.setCellStyle(editableCellStyle);

			// Address Line 2 (column 8) - Editable
			Cell address2Cell = row.createCell(8);
			address2Cell.setCellValue(coach.getAddressLine2());
			address2Cell.setCellStyle(editableCellStyle);

			// Pincode (column 9) - Editable
			Cell pincodeCell = row.createCell(9);
			pincodeCell.setCellValue(coach.getPincode());
			pincodeCell.setCellStyle(editableCellStyle);

			// City (column 10) - Editable
			Cell cityCell = row.createCell(10);
			cityCell.setCellValue(coach.getCity());
			cityCell.setCellStyle(editableCellStyle);

			// State (column 11) - Editable
			Cell stateCell = row.createCell(11);
			stateCell.setCellValue(coach.getState());
			stateCell.setCellStyle(editableCellStyle);

			// Country (column 12) - Editable
			Cell countryCell = row.createCell(12);
			countryCell.setCellValue(coach.getCountry());
			countryCell.setCellStyle(editableCellStyle);

			// ID (column 13) - Hidden and Locked
			Cell idCell = row.createCell(13);
			idCell.setCellValue(coach.getId() != null ? coach.getId().toString() : "");
			idCell.setCellStyle(lockedCellStyle);
		}

		applyFieldValidationsWithData(sheet);
		autoSizeColumns(sheet, COACH_TEMPLATE_HEADERS_WITH_ID.length - 1); // Don't auto-size ID column
		sheet.createFreezePane(0, 1);

		// Hide the ID column (column 13)
		sheet.setColumnHidden(13, true);

		// Protect the sheet
		sheet.protectSheet("");

		return workbook;
	}

	public static Workbook generateCoachTemplate() {
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Coach List");

		// Create header row without ID column
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < COACH_TEMPLATE_HEADERS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(COACH_TEMPLATE_HEADERS[i]);
		}

		applyFieldValidationsForTemplate(sheet);
		autoSizeColumns(sheet, COACH_TEMPLATE_HEADERS.length);
		sheet.createFreezePane(0, 1);

		return workbook;
	}

	private static void autoSizeColumns(Sheet sheet, int columnCount) {
		for (int i = 0; i < columnCount; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	private static void applyFieldValidationsWithData(Sheet sheet) {
		// Name and Phone are locked, so no validation needed for editing

		// Designation (column 2) - Editable, optional text
		createOptionalTextValidation(sheet, 1, 2);

		// Email (column 3) - Editable with email validation
		createEmailValidation(sheet, 1, 3);

		// Gender (column 4) - Editable dropdown
		createDropdownValidation(sheet, 1, 4, Gender.values());

		// Role (column 5) - Editable dropdown
		createDropdownValidation(sheet, 1, 5, getAllowedRoles());

		// Experience (column 6) - Editable numeric
		createNumericValidation(sheet, 1, 6);

		// Address Line 1 (column 7) - Editable required text
		createRequiredTextValidation(sheet, 1, 7);

		// Address Line 2 (column 8) - Editable optional text
		createOptionalTextValidation(sheet, 1, 8);

		// Pincode (column 9) - Editable with pincode validation
		createPincodeValidation(sheet, 1, 9);

		// City (column 10) - Editable required text
		createRequiredTextValidation(sheet, 1, 10);

		// State (column 11) - Editable required text
		createRequiredTextValidation(sheet, 1, 11);

		// Country (column 12) - Editable optional text
		createOptionalTextValidation(sheet, 1, 12);

		// No validation for ID column as it's hidden and locked
	}

	private static void applyFieldValidationsForTemplate(Sheet sheet) {
		// Name (column 0) - Required text for new entries
		createRequiredTextValidation(sheet, 1, 0);

		// Phone Number (column 1) - Required phone validation for new entries
		createPhoneValidation(sheet, 1, 1);

		// Designation (column 2) - Optional text
		createOptionalTextValidation(sheet, 1, 2);

		// Email (column 3) - Email validation
		createEmailValidation(sheet, 1, 3);

		// Gender (column 4) - Dropdown
		createDropdownValidation(sheet, 1, 4, Gender.values());

		// Role (column 5) - Dropdown
		createDropdownValidation(sheet, 1, 5, getAllowedRoles());

		// Experience (column 6) - Numeric
		createNumericValidation(sheet, 1, 6);

		// Address Line 1 (column 7) - Required text
		createRequiredTextValidation(sheet, 1, 7);

		// Address Line 2 (column 8) - Optional text
		createOptionalTextValidation(sheet, 1, 8);

		// Pincode (column 9) - Pincode validation
		createPincodeValidation(sheet, 1, 9);

		// City (column 10) - Required text
		createRequiredTextValidation(sheet, 1, 10);

		// State (column 11) - Required text
		createRequiredTextValidation(sheet, 1, 11);

		// Country (column 12) - Optional text
		createOptionalTextValidation(sheet, 1, 12);
	}

	private static Role[] getAllowedRoles() {
		return Arrays.stream(Role.values()).filter(role -> role != Role.USER && role != Role.SUPER_ADMIN
				&& role != Role.ACADEMY_OWNER && role != Role.PLAYER).toArray(Role[]::new);
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

	private static void createRequiredTextValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		String formula = "AND(ISTEXT(RC), LEN(TRIM(RC)) > 0)";
		applyValidation(sheet, helper.createCustomConstraint(formula), startRow, col, "Required Field",
				"This field cannot be empty");
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

	private static void createNumericValidation(Sheet sheet, int startRow, int col) {
		DataValidationHelper helper = sheet.getDataValidationHelper();
		DataValidationConstraint constraint = helper.createNumericConstraint(
				DataValidationConstraint.ValidationType.DECIMAL, DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
				"0", null);
		CellRangeAddressList range = new CellRangeAddressList(startRow, 1048575, col, col);
		DataValidation validation = helper.createValidation(constraint, range);
		validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		validation.setShowErrorBox(true);
		validation.createErrorBox("Invalid Number", "Must be a positive number (e.g., 24)");
		validation.setShowPromptBox(true);
		validation.createPromptBox("Experience in Months", "Enter coach's experience in months (e.g., 24 for 2 years)");
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