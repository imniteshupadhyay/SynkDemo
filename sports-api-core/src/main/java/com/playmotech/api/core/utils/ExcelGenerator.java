package com.playmotech.api.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playmotech.api.core.response.dao.DuePaymentsViewDao;

import jakarta.servlet.http.HttpServletResponse;

public class ExcelGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Method for direct HTTP response export
	public static <T> void export(List<T> daos, HttpServletResponse response, String fileName) throws IOException {
		if (daos == null || daos.isEmpty()) {
			throw new IllegalArgumentException("DAO list cannot be null or empty");
		}

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

		byte[] excelData = generateExcel(daos, null);
		response.getOutputStream().write(excelData);
	}

	// Main method for generating Excel - handles different types
	public static byte[] generateDueExcel(List<DuePaymentsViewDao> duePaymentsList, String templateName) {
		if (duePaymentsList == null || duePaymentsList.isEmpty()) {
			throw new IllegalArgumentException("DAO list cannot be null or empty");
		}

		if ("due_payments".equals(templateName) || templateName == null) {
			return generateDuePaymentsExcel(duePaymentsList);
		}

		throw new IllegalArgumentException("Unknown template: " + templateName);
	}

	// Generic Excel generation method for other types
	public static <T> byte[] generateExcel(List<T> daos, List<String> columnOrder) throws IOException {
		if (daos == null || daos.isEmpty()) {
			throw new IllegalArgumentException("DAO list cannot be null or empty");
		}

		Class<?> daoClass = daos.get(0).getClass();
		List<Method> allGetters = getGetters(daoClass);
		List<Method> getters;

		if (columnOrder != null && !columnOrder.isEmpty()) {
			getters = columnOrder.stream().map(field -> findGetterByFieldName(allGetters, field))
					.collect(Collectors.toList());
		} else {
			getters = allGetters;
		}

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Data");

			// Header row
			Row headerRow = sheet.createRow(0);
			int colIdx = 0;
			for (Method getter : getters) {
				String columnName = resolveColumnName(getter);
				headerRow.createCell(colIdx++).setCellValue(columnName);
			}

			// Data rows
			int rowIdx = 1;
			for (T dao : daos) {
				Row row = sheet.createRow(rowIdx++);
				colIdx = 0;
				for (Method getter : getters) {
					Object value = invokeGetter(getter, dao);
					setCellValue(row.createCell(colIdx++), value);
				}
			}

			// Auto-size columns to fit content
			for (int i = 0; i < getters.size(); i++) {
				sheet.autoSizeColumn(i);
			}

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				workbook.write(outputStream);
				return outputStream.toByteArray();
			}
		} catch (Exception e) {
			throw new IOException("Failed to generate Excel file", e);
		}
	}

	// Specific method for DuePaymentsViewDao with custom formatting
	public static byte[] generateDuePaymentsExcel(List<DuePaymentsViewDao> duePaymentsList) {
		try (XSSFWorkbook workbook = new XSSFWorkbook();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet("Due Payments Report");

			// Create header styles
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle subHeaderStyle = createSubHeaderStyle(workbook);
			CellStyle dataStyle = createDataStyle(workbook);
			CellStyle wrappedDataStyle = createWrappedDataStyle(workbook);
			CellStyle numberStyle = createNumberStyle(workbook);
			CellStyle dateStyle = createDateStyle(workbook);

			// Create headers
			createHeaders(sheet, headerStyle, subHeaderStyle);

			// Fill data
			fillData(sheet, duePaymentsList, dataStyle, wrappedDataStyle, numberStyle, dateStyle);

			// Auto-fit columns to max content
			autoSizeAllColumns(sheet);

			workbook.write(outputStream);
			return outputStream.toByteArray();

		} catch (Exception e) {
			throw new RuntimeException("Failed to generate Excel file", e);
		}
	}

	private static void createHeaders(Sheet sheet, CellStyle headerStyle, CellStyle subHeaderStyle) {
		// Create header row
		Row headerRow = sheet.createRow(0);
		int colIndex = 0;

		// Player Details
		createCell(headerRow, colIndex++, "Player Name", headerStyle);
		createCell(headerRow, colIndex++, "Player Email", headerStyle);
		createCell(headerRow, colIndex++, "Player Phone", headerStyle);

		// Academy & Program Details (Split into separate columns)
		createCell(headerRow, colIndex++, "Academy Name", headerStyle);
		createCell(headerRow, colIndex++, "Program Name", headerStyle);
		createCell(headerRow, colIndex++, "Sport", headerStyle);
		createCell(headerRow, colIndex++, "Payment Schedule", headerStyle);
		createCell(headerRow, colIndex++, "Joining Date", headerStyle);

		// Registration Fee
		createCell(headerRow, colIndex++, "Registration Fee - Pending", headerStyle);
		createCell(headerRow, colIndex++, "Registration Fee - Paid", headerStyle);

		// Program Fee
		createCell(headerRow, colIndex++, "Program Fee - Pending", headerStyle);
		createCell(headerRow, colIndex++, "Program Fee - Current Due", headerStyle);
		createCell(headerRow, colIndex++, "Program Fee - Due Date", headerStyle); // Combined due date column
		createCell(headerRow, colIndex++, "Program Fee - Total Due", headerStyle);
		createCell(headerRow, colIndex++, "Program Fee - Paid", headerStyle);

		// Pending Registration Details
		createCell(headerRow, colIndex++, "Pending Registration Details", headerStyle);

		// Pending Program Details
		createCell(headerRow, colIndex++, "Pending Program Details", headerStyle);

		// Status Overview
		createCell(headerRow, colIndex++, "Enrollment Status", headerStyle);
		createCell(headerRow, colIndex++, "Payment Status", headerStyle);

		// Due Status
		createCell(headerRow, colIndex++, "Registration Due Status", headerStyle);
		createCell(headerRow, colIndex++, "Registration Due Days Count", headerStyle);
		createCell(headerRow, colIndex++, "Course Due Status", headerStyle);
		createCell(headerRow, colIndex++, "Course Due Days Count", headerStyle);
	}

	private static void fillData(Sheet sheet, List<DuePaymentsViewDao> duePaymentsList, CellStyle dataStyle,
			CellStyle wrappedDataStyle, CellStyle numberStyle, CellStyle dateStyle) {
		int rowIndex = 1;

		for (DuePaymentsViewDao dao : duePaymentsList) {
			Row dataRow = sheet.createRow(rowIndex++);
			int colIndex = 0;

			// Player Details
			createCell(dataRow, colIndex++, dao.getPlayerName(), dataStyle);
			createCell(dataRow, colIndex++, dao.getPlayerEmailId(), dataStyle);
			createCell(dataRow, colIndex++, dao.getPlayerContactNumber(), dataStyle);

			// Academy & Program Details
			createCell(dataRow, colIndex++, dao.getAcademyName(), dataStyle);
			createCell(dataRow, colIndex++, dao.getCourseName(), dataStyle);
			createCell(dataRow, colIndex++, dao.getSport(), dataStyle);
			createCell(dataRow, colIndex++, formatPaymentSchedule(dao.getPaymentSchedule()), dataStyle);
			createCell(dataRow, colIndex++,
					dao.getJoiningDate() != null ? dao.getJoiningDate().format(DATE_FORMATTER) : "N/A", dateStyle);

			// Registration Fee (without currency symbol)
			createNumberCell(dataRow, colIndex++, dao.getRegistrationTotalPending(), numberStyle);
			createNumberCell(dataRow, colIndex++, dao.getRegistrationTotalPaid(), numberStyle);

			// Program Fee (without currency symbol)
			createNumberCell(dataRow, colIndex++, dao.getCourseTotalPending(), numberStyle);
			createNumberCell(dataRow, colIndex++, dao.getCurrentCourseDue(), numberStyle);
			createCell(dataRow, colIndex++, dao.getDuesOn() != null ? dao.getDuesOn().format(DATE_FORMATTER) : "N/A",
					dateStyle); // Single due date column
			createNumberCell(dataRow, colIndex++, dao.getTotalCourseDue(), numberStyle);
			createNumberCell(dataRow, colIndex++, dao.getCourseTotalPaid(), numberStyle);

			// Pending Details - Use wrapped style for better readability
			createCell(dataRow, colIndex++, formatPendingDetails(dao.getPendingRegistrationPaymentDetails()),
					wrappedDataStyle);
			createCell(dataRow, colIndex++, formatPendingDetails(dao.getPendingCoursePaymentDetails()),
					wrappedDataStyle);

			// Status Overview
			createCell(dataRow, colIndex++, formatEnrollmentStatus(dao.getStatus()), dataStyle);
			createCell(dataRow, colIndex++, dao.getPaymentStatus(), dataStyle);

			// Due Status
			createCell(dataRow, colIndex++, dao.getRegistrationDueDaysStatus(), dataStyle);
			createCell(dataRow, colIndex++, dao.getRegistrationDueDaysCount(), dataStyle);
			createCell(dataRow, colIndex++, dao.getCourseDueDaysStatus(), dataStyle);
			createCell(dataRow, colIndex++, dao.getCourseDueDaysCount(), dataStyle);
		}
	}

	// Auto-size all columns to fit content
	private static void autoSizeAllColumns(Sheet sheet) {
		if (sheet.getPhysicalNumberOfRows() > 0) {
			Row headerRow = sheet.getRow(0);
			if (headerRow != null) {
				int numberOfColumns = headerRow.getPhysicalNumberOfCells();
				for (int i = 0; i < numberOfColumns; i++) {
					sheet.autoSizeColumn(i);
				}
			}
		}
	}

	// Formatting helper methods
	private static String formatPaymentSchedule(String paymentSchedule) {
		if (paymentSchedule == null)
			return "N/A";
		return capitalizeWords(paymentSchedule.replace("_", " ").toLowerCase());
	}

	private static String formatEnrollmentStatus(String status) {
		if (status == null)
			return "N/A";
		return capitalizeWords(status.replace("_", " ").toLowerCase());
	}

	private static String capitalizeWords(String input) {
		if (input == null || input.isEmpty())
			return input;

		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = true;

		for (char c : input.toCharArray()) {
			if (Character.isWhitespace(c)) {
				capitalizeNext = true;
				result.append(c);
			} else if (capitalizeNext) {
				result.append(Character.toUpperCase(c));
				capitalizeNext = false;
			} else {
				result.append(c);
			}
		}

		return result.toString();
	}

	private static String formatPendingDetails(String pendingDetailsJson) {
		if (pendingDetailsJson == null || pendingDetailsJson.trim().isEmpty()) {
			return "No pending payments";
		}

		try {
			Map<String, Object> details = objectMapper.readValue(pendingDetailsJson,
					new TypeReference<Map<String, Object>>() {
					});

			if (details.isEmpty()) {
				return "No pending payments";
			}

			StringBuilder sb = new StringBuilder();
			details.forEach((date, amount) -> {
				if (sb.length() > 0)
					sb.append(";\n"); // Add line break after semicolon
				sb.append(formatDateFromString(date)).append(": ").append(amount);
			});

			return sb.toString();
		} catch (Exception e) {
			return "Invalid format";
		}
	}

	private static String formatDateFromString(String dateStr) {
		try {
			// Handle both ISO date and datetime formats
			if (dateStr.contains(" ")) {
				// Handle datetime format like "2025-01-15 00:00:00"
				LocalDate date = LocalDate.parse(dateStr.split(" ")[0]);
				return date.format(DATE_FORMATTER);
			} else {
				// Handle ISO date format like "2025-01-15"
				LocalDate date = LocalDate.parse(dateStr);
				return date.format(DATE_FORMATTER);
			}
		} catch (Exception e) {
			return dateStr;
		}
	}

	// Cell creation helper methods
	private static void createCell(Row row, int columnIndex, Object value, CellStyle style) {
		Cell cell = row.createCell(columnIndex);
		if (value != null) {
			if (value instanceof Number) {
				cell.setCellValue(((Number) value).doubleValue());
			} else {
				cell.setCellValue(value.toString());
			}
		} else {
			cell.setCellValue("N/A");
		}
		cell.setCellStyle(style);
	}

	private static void createNumberCell(Row row, int columnIndex, Double value, CellStyle style) {
		Cell cell = row.createCell(columnIndex);
		if (value != null) {
			cell.setCellValue(value);
		} else {
			cell.setCellValue(0.0);
		}
		cell.setCellStyle(style);
	}

	// Generic helper methods for reflection-based Excel generation
	private static Method findGetterByFieldName(List<Method> getters, String fieldName) {
		String capitalized = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		String getterName = "get" + capitalized;
		String isGetterName = "is" + capitalized;

		return getters.stream().filter(m -> m.getName().equals(getterName) || m.getName().equals(isGetterName))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("No getter found for field: " + fieldName));
	}

	private static String getFieldFormat(Class<?> clazz, String fieldName) {
		try {
			java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
			ColumnHeader annotation = field.getAnnotation(ColumnHeader.class);
			if (annotation != null && !annotation.format().isEmpty()) {
				return annotation.format();
			}
		} catch (NoSuchFieldException e) {
			// Field not found, return null
		}
		return null;
	}

	private static List<Method> getGetters(Class<?> clazz) {
		return Arrays.stream(clazz.getMethods())
				.filter(method -> (method.getName().startsWith("get") && method.getParameterCount() == 0
						&& !method.getName().equals("getClass"))
						|| (method.getName().startsWith("is") && method.getParameterCount() == 0
								&& method.getReturnType() == boolean.class))
				.sorted(Comparator.comparing(Method::getName)).collect(Collectors.toList());
	}

	private static String resolveColumnName(Method getter) {
		try {
			// First try to get the column name from the field's ColumnHeader annotation
			String fieldName = getter.getName();
			if (fieldName.startsWith("get") && fieldName.length() > 3) {
				fieldName = fieldName.substring(3);
			} else if (fieldName.startsWith("is") && fieldName.length() > 2) {
				fieldName = fieldName.substring(2);
			}

			// Convert first character to lowercase
			if (!fieldName.isEmpty()) {
				fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);

				// Try to get the field and its annotation
				try {
					Field field = getter.getDeclaringClass().getDeclaredField(fieldName);
					ColumnHeader annotation = field.getAnnotation(ColumnHeader.class);
					if (annotation != null) {
						return annotation.value();
					}
				} catch (NoSuchFieldException e) {
					// Field not found, use default naming
				}
			}

			// Default to property name if no annotation found
			return fieldName;
		} catch (Exception e) {
			// Fallback to simple method name extraction
			String name = getter.getName();
			if (name.startsWith("get") && name.length() > 3) {
				name = name.substring(3);
			} else if (name.startsWith("is") && name.length() > 2) {
				name = name.substring(2);
			}
			return name;
		}
	}

	private static Object invokeGetter(Method getter, Object target) {
		try {
			return getter.invoke(target);
		} catch (Exception e) {
			throw new RuntimeException("Error invoking getter method", e);
		}
	}

	private static void setCellValue(Cell cell, Object value) {
		if (value == null) {
			cell.setCellValue("");
			return;
		}

		try {
			// First try to handle dates with cell styles
			if (value instanceof LocalDate) {
				LocalDate dateValue = (LocalDate) value;
				cell.setCellValue(dateValue);
				// Apply date format to cell
				CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
				cellStyle.setDataFormat(cell.getSheet().getWorkbook()
					.createDataFormat()
					.getFormat("dd-mmm-yyyy"));
				cell.setCellStyle(cellStyle);
			} else if (value instanceof LocalDateTime) {
				LocalDateTime dateTimeValue = (LocalDateTime) value;
				cell.setCellValue(dateTimeValue);
				// Apply date-time format to cell
				CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
				cellStyle.setDataFormat(cell.getSheet().getWorkbook()
					.createDataFormat()
					.getFormat("dd-mmm-yyyy hh:mm AM/PM"));
				cell.setCellStyle(cellStyle);
			} else if (value instanceof java.util.Date) {
				Date dateValue = (Date) value;
				cell.setCellValue(dateValue);
			} else if (value instanceof java.sql.Timestamp) {
				Timestamp timestamp = (Timestamp) value;
				cell.setCellValue(timestamp);
				// Apply date-time format to cell
				CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
				cellStyle.setDataFormat(cell.getSheet().getWorkbook()
					.createDataFormat()
					.getFormat("dd-mmm-yyyy hh:mm AM/PM"));
				cell.setCellStyle(cellStyle);
			} else if (value instanceof String) {
				cell.setCellValue((String) value);
			} else if (value instanceof Number) {
				cell.setCellValue(((Number) value).doubleValue());
			} else if (value instanceof Boolean) {
				cell.setCellValue((Boolean) value);
			} else {
				cell.setCellValue(value.toString());
			}
		} catch (Exception e) {
			// Fallback to string representation if any error occurs
			cell.setCellValue(value != null ? value.toString() : "");
		}
	}

	// Style creation methods
	private static CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		font.setFontHeightInPoints((short) 12);
		style.setFont(font);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return style;
	}

	private static CellStyle createSubHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		font.setFontHeightInPoints((short) 10);
		style.setFont(font);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setAlignment(HorizontalAlignment.CENTER);
		return style;
	}

	private static CellStyle createDataStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return style;
	}

	private static CellStyle createWrappedDataStyle(Workbook workbook) {
		CellStyle style = createDataStyle(workbook);
		style.setWrapText(true);
		style.setVerticalAlignment(VerticalAlignment.TOP);
		return style;
	}

	private static CellStyle createNumberStyle(Workbook workbook) {
		CellStyle style = createDataStyle(workbook);
		DataFormat format = workbook.createDataFormat();
		style.setDataFormat(format.getFormat("#,##0.00")); // Number format without currency
		style.setAlignment(HorizontalAlignment.RIGHT);
		return style;
	}

	private static CellStyle createDateStyle(Workbook workbook) {
		CellStyle style = createDataStyle(workbook);
		style.setAlignment(HorizontalAlignment.CENTER);
		return style;
	}
}