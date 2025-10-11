package com.playmotech.api.core.utils.excel;

import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.views.TraineeView;

public class ProgramPlayerExcelTemplateGenerator {

	private static final String[] PROGRAM_PLAYER_MAPPING_HEADERS = { "Program*", "Player*", "Payment Schedule*",
			"Amount*", "Joining Date* (dd-MM-yyyy)", "Due Date (dd-MM-yyyy)" };

	public static Workbook generateProgramPlayerTemplate(List<CourseDto> courses, List<TraineeView> trainees) {
		XSSFWorkbook workbook = new XSSFWorkbook();

		// Create a hidden sheet for data validation lists
		Sheet validationSheet = workbook.createSheet("ValidationData");
		int validationSheetIndex = workbook.getSheetIndex(validationSheet);
		workbook.setSheetHidden(validationSheetIndex, true);

		// Create the visible mapping sheet
		XSSFSheet mappingSheet = workbook.createSheet("Program-Player Mapping");

		// Populate validation sheet with programs, trainees and payment schedules data
		populateValidationData(workbook, validationSheet, courses, trainees);

		// Create header row for mapping sheet
		Row headerRow = mappingSheet.createRow(0);
		CellStyle headerStyle = createHeaderStyle(workbook);

		for (int i = 0; i < PROGRAM_PLAYER_MAPPING_HEADERS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(PROGRAM_PLAYER_MAPPING_HEADERS[i]);
			cell.setCellStyle(headerStyle);
		}

		// Apply data validations for program, player, and payment schedule columns
		applyDataValidations(workbook, mappingSheet, courses, trainees);

		// Auto-size all columns
		for (int i = 0; i < PROGRAM_PLAYER_MAPPING_HEADERS.length; i++) {
			mappingSheet.autoSizeColumn(i);
		}

		// Freeze the header row
		mappingSheet.createFreezePane(0, 1);

		return workbook;
	}

	private static void populateValidationData(Workbook workbook, Sheet validationSheet, List<CourseDto> courses,
			List<TraineeView> trainees) {
		Row headerRow = validationSheet.createRow(0);

		// Programs section (Column A-B)
		headerRow.createCell(0).setCellValue("Program Name");
		headerRow.createCell(1).setCellValue("Program ID");

		// Populate program data
		for (int i = 0; i < courses.size(); i++) {
			CourseDto course = courses.get(i);
			Row row = validationSheet.createRow(i + 1);
			row.createCell(0).setCellValue(course.getTitle());
			row.createCell(1).setCellValue(course.getId());
		}

		// Create named range for program names
		Name programNamesRange = workbook.createName();
		programNamesRange.setNameName("ProgramNames");
		programNamesRange.setRefersToFormula("ValidationData!$A$2:$A$" + (courses.size() + 1));

		// Players section (Column D-E)
		headerRow.createCell(3).setCellValue("Player Name");
		headerRow.createCell(4).setCellValue("Player ID");

		// Populate player data
		for (int i = 0; i < trainees.size(); i++) {
			TraineeView trainee = trainees.get(i);
			Row row = validationSheet.getRow(i + 1);
			if (row == null) {
				row = validationSheet.createRow(i + 1);
			}
			row.createCell(3).setCellValue(trainee.getDisplayName());
			row.createCell(4).setCellValue(trainee.getId());
		}

		// Create named range for player names
		Name playerNamesRange = workbook.createName();
		playerNamesRange.setNameName("PlayerNames");
		playerNamesRange.setRefersToFormula("ValidationData!$D$2:$D$" + (trainees.size() + 1));

		// Create helper sheet for program-specific payment schedules
		Sheet helperSheet = workbook.createSheet("Helper");
		workbook.setSheetHidden(workbook.getSheetIndex(helperSheet), true);
		setupHelperSheet(helperSheet, courses);
	}

	private static void setupHelperSheet(Sheet helperSheet, List<CourseDto> courses) {
		// Create header row for helper sheet
		Row headerRow = helperSheet.createRow(0);
		headerRow.createCell(0).setCellValue("Program Name");
		headerRow.createCell(1).setCellValue("Program ID");
		headerRow.createCell(2).setCellValue("Payment Schedule");
		headerRow.createCell(3).setCellValue("Amount");

		int rowIndex = 1;

		// For each program, create entries for each payment schedule
		for (CourseDto course : courses) {
			if (course.getPaymentOptions() != null && !course.getPaymentOptions().isEmpty()) {
				for (Map.Entry<PaymentSchedule, Long> entry : course.getPaymentOptions().entrySet()) {
					Row row = helperSheet.createRow(rowIndex++);
					row.createCell(0).setCellValue(course.getTitle());
					row.createCell(1).setCellValue(course.getId());
					row.createCell(2).setCellValue(entry.getKey().name());
					row.createCell(3).setCellValue(entry.getValue());
				}
			}
		}

		// Create named ranges for each program's payment schedules
		for (CourseDto course : courses) {
			String programName = course.getTitle().replaceAll("[^a-zA-Z0-9]", "_");

			// Create a named range for this program's payment schedules
			int startRow = -1;
			int endRow = -1;

			// Find the rows containing this program's payment schedules
			for (int row = 1; row < helperSheet.getLastRowNum() + 1; row++) {
				Row currentRow = helperSheet.getRow(row);
				if (currentRow != null && currentRow.getCell(0) != null) {
					String cellValue = currentRow.getCell(0).getStringCellValue();
					if (cellValue.equals(course.getTitle())) {
						if (startRow == -1) {
							startRow = row + 1; // +1 because Excel is 1-based
						}
						endRow = row + 1;
					}
				}
			}

			if (startRow != -1 && endRow != -1) {
				Name programSchedules = helperSheet.getWorkbook().createName();
				programSchedules.setNameName("Program_" + programName + "_Schedules");
				programSchedules.setRefersToFormula("Helper!$C$" + startRow + ":$C$" + endRow);

				// Also create a range for the amounts
				Name programAmounts = helperSheet.getWorkbook().createName();
				programAmounts.setNameName("Program_" + programName + "_Amounts");
				programAmounts.setRefersToFormula("Helper!$C$" + startRow + ":$D$" + endRow);
			}
		}
	}

	private static void applyDataValidations(Workbook workbook, Sheet sheet, List<CourseDto> courses,
			List<TraineeView> trainees) {
		DataValidationHelper dvHelper = sheet.getDataValidationHelper();

		// Program dropdown validation (Column A)
		DataValidationConstraint programConstraint = dvHelper.createFormulaListConstraint("ProgramNames");
		CellRangeAddressList programAddressList = new CellRangeAddressList(1, 1000, 0, 0);
		DataValidation programValidation = dvHelper.createValidation(programConstraint, programAddressList);
		programValidation.setShowErrorBox(true);
		programValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		programValidation.createErrorBox("Invalid Selection", "Please select a valid program from the dropdown list");
		sheet.addValidationData(programValidation);

		// Player dropdown validation (Column B)
		DataValidationConstraint playerConstraint = dvHelper.createFormulaListConstraint("PlayerNames");
		CellRangeAddressList playerAddressList = new CellRangeAddressList(1, 1000, 1, 1);
		DataValidation playerValidation = dvHelper.createValidation(playerConstraint, playerAddressList);
		playerValidation.setShowErrorBox(true);
		playerValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
		playerValidation.createErrorBox("Invalid Selection", "Please select a valid player from the dropdown list");
		sheet.addValidationData(playerValidation);

		// Create payment schedule validation on each row
		for (int rowNum = 1; rowNum <= 1000; rowNum++) {
			Row row = sheet.getRow(rowNum);
			if (row == null) {
				row = sheet.createRow(rowNum);
			}

			// Add hidden formula cells for lookup purposes
			Cell programNameCell = row.createCell(6);
			programNameCell.setCellFormula("A" + (rowNum + 1));
			sheet.setColumnHidden(6, true);

			Cell programIdCell = row.createCell(7);
			programIdCell.setCellFormula(
					"VLOOKUP(A" + (rowNum + 1) + ",ValidationData!$A$2:$B$" + (courses.size() + 1) + ",2,FALSE)");
			sheet.setColumnHidden(7, true);

			Cell playerIdCell = row.createCell(8);
			playerIdCell.setCellFormula(
					"VLOOKUP(B" + (rowNum + 1) + ",ValidationData!$D$2:$E$" + (trainees.size() + 1) + ",2,FALSE)");
			sheet.setColumnHidden(8, true);

			// Create dynamic validation for payment schedules
			// For each row, create a validation that checks the program and shows
			// appropriate options
			for (CourseDto course : courses) {
				String programName = course.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
				String validationFormula = "INDIRECT(\"Program_\" & SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(A" + (rowNum + 1)
						+ ", \" \", \"_\"), \".\", \"_\"), \"-\", \"_\") & \"_Schedules\")";

				DataValidationConstraint scheduleConstraint = dvHelper.createFormulaListConstraint(validationFormula);
				CellRangeAddressList scheduleAddressList = new CellRangeAddressList(rowNum, rowNum, 2, 2);
				DataValidation scheduleValidation = dvHelper.createValidation(scheduleConstraint, scheduleAddressList);
				scheduleValidation.setShowErrorBox(true);
				scheduleValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
				scheduleValidation.createErrorBox("Invalid Selection", "Please select a valid payment schedule");
				sheet.addValidationData(scheduleValidation);

				// Formula to auto-populate the amount based on the selected program and payment
				// schedule
				Cell amountCell = row.createCell(3);
				String amountFormula = "IFERROR(VLOOKUP(C" + (rowNum + 1) + ","
						+ "INDIRECT(\"Program_\" & SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(A" + (rowNum + 1)
						+ ", \" \", \"_\"), \".\", \"_\"), \"-\", \"_\") & \"_Amounts\"),2,FALSE),\"\")";
				amountCell.setCellFormula(amountFormula);
			}
		}
	}

	private static CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}
}