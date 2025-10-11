package com.playmotech.api.core.utils.excel;

import java.util.List;

import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.playmotech.api.core.dto.CoachDetailsDto;
import com.playmotech.api.core.dto.CourseDto;

public class ProgramCoachExcelTemplateGenerator {

	private static final String[] VISIBLE_HEADERS = { "Program*", "Coach*" }; // No hidden columns

	public static Workbook generateProgramCoachTemplate(List<CourseDto> courses, List<CoachDetailsDto> coaches) {
		Workbook workbook = new XSSFWorkbook();

		// Hidden validation sheet to store names and IDs
		Sheet validationSheet = workbook.createSheet("ValidationData");
		workbook.setSheetHidden(workbook.getSheetIndex(validationSheet), true);

		// Populate programs (Column A: Name, Column B: ID)
		populateValidationData(validationSheet, courses, 0, "Program");

		// Populate coaches (Column D: Name, Column E: ID)
		populateValidationData(validationSheet, coaches, 3, "Coach");

		// Visible mapping sheet
		Sheet mappingSheet = workbook.createSheet("Program-Coach Mapping");
		createHeaderRow(mappingSheet, VISIBLE_HEADERS);

		// Apply data validation (dropdowns)
		applyDataValidation(workbook, mappingSheet, courses.size(), coaches.size());

		return workbook;
	}

	// Helper to populate validation data (for programs or coaches)
	private static void populateValidationData(Sheet sheet, List<?> items, int startCol, String type) {
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(startCol).setCellValue(type + " Name");
		headerRow.createCell(startCol + 1).setCellValue(type + " ID");

		for (int i = 0; i < items.size(); i++) {
			Row row = sheet.createRow(i + 1);
			if (items.get(i) instanceof CourseDto) {
				CourseDto course = (CourseDto) items.get(i);
				row.createCell(startCol).setCellValue(course.getTitle());
				row.createCell(startCol + 1).setCellValue(course.getId());
			} else if (items.get(i) instanceof CoachDetailsDto) {
				CoachDetailsDto coach = (CoachDetailsDto) items.get(i);
				row.createCell(startCol).setCellValue(coach.getUserProfile().getDisplayName());
				row.createCell(startCol + 1).setCellValue(coach.getCoachUserId());
			}
		}
	}

	// Create header row for the mapping sheet
	private static void createHeaderRow(Sheet sheet, String[] headers) {
		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			headerRow.createCell(i).setCellValue(headers[i]);
		}
	}

	// Apply dropdown validation using named ranges
	private static void applyDataValidation(Workbook workbook, Sheet mappingSheet, int programCount, int coachCount) {
		// Programs dropdown (Column A)
		Name programNames = workbook.createName();
		programNames.setNameName("ProgramNames");
		programNames.setRefersToFormula("ValidationData!$A$2:$A$" + (programCount + 1));

		DataValidationHelper dvHelper = mappingSheet.getDataValidationHelper();
		DataValidationConstraint programConstraint = dvHelper.createFormulaListConstraint("ProgramNames");
		mappingSheet.addValidationData(
				dvHelper.createValidation(programConstraint, new CellRangeAddressList(1, 1000, 0, 0)));

		// Coaches dropdown (Column B)
		Name coachNames = workbook.createName();
		coachNames.setNameName("CoachNames");
		coachNames.setRefersToFormula("ValidationData!$D$2:$D$" + (coachCount + 1));

		DataValidationConstraint coachConstraint = dvHelper.createFormulaListConstraint("CoachNames");
		mappingSheet
				.addValidationData(dvHelper.createValidation(coachConstraint, new CellRangeAddressList(1, 1000, 1, 1)));
	}
}