package com.playmotech.api.core.utils.excel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.excel.ParsedRow;
import com.playmotech.api.core.response.excel.RowError;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class BulkUploaderHelper {

	public void processParsingErrors(List<ParsedRow> parsedRows, List<RowError> allErrors) {
		parsedRows.stream().filter(ParsedRow::hasParsingErrors).forEach(row -> {
			log.warn("Parsing error at row {}: {}", row.getRowNumber(), row.getParsingErrors());

			List<String> structuredErrors = row.getParsingErrors().stream()
					.map(error -> enhanceParsingErrorWithStructure(error, row)).collect(Collectors.toList());

			allErrors.add(new RowError(row.getRowNumber(), structuredErrors));
		});
	}

	public String enhanceParsingErrorWithStructure(String originalError, ParsedRow row) {
		// If already structured, return as-is
		if (originalError.startsWith("FIELD:") || originalError.startsWith("GENERAL:")) {
			return originalError;
		}

		// Try to identify field-specific parsing errors based on common patterns
		String structuredError = identifyFieldSpecificParsingError(originalError, row);
		if (structuredError != null) {
			return structuredError;
		}

		// Default to general parsing error
		return createGeneralError("PARSING_ERROR", "File parsing failed: " + originalError);
	}

	private String identifyFieldSpecificParsingError(String originalError, ParsedRow row) {
		String lowerError = originalError.toLowerCase();

		// Common Excel parsing error patterns and their field mappings
		if (lowerError.contains("phone") || lowerError.contains("mobile")) {
			return createFieldError("Phone Number", "INVALID_FORMAT",
					"Invalid phone number format: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("email")) {
			return createFieldError("Email", "INVALID_FORMAT",
					"Invalid email format: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("name") || lowerError.contains("display")) {
			return createFieldError("Name", "INVALID_FORMAT",
					"Invalid name format: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("date") || lowerError.contains("birth")) {
			return createFieldError("Date of Birth", "INVALID_FORMAT",
					"Invalid date format: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("role")) {
			return createFieldError("Role", "INVALID_VALUE",
					"Invalid role value: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("experience")) {
			return createFieldError("Experience", "INVALID_FORMAT",
					"Invalid experience format: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("designation")) {
			return createFieldError("Designation", "INVALID_FORMAT",
					"Invalid designation format: " + extractRelevantPart(originalError));
		}

		// Column-specific errors (if your parsing includes column references)
		if (lowerError.contains("column a") || lowerError.contains("col a")) {
			return createFieldError("Name", "PARSING_ERROR",
					"Error reading name from Excel: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("column b") || lowerError.contains("col b")) {
			return createFieldError("Phone Number", "PARSING_ERROR",
					"Error reading phone number from Excel: " + extractRelevantPart(originalError));
		}

		if (lowerError.contains("column c") || lowerError.contains("col c")) {
			return createFieldError("Email", "PARSING_ERROR",
					"Error reading email from Excel: " + extractRelevantPart(originalError));
		}

		// Excel-specific errors
		if (lowerError.contains("merged cell")) {
			return createGeneralError("MERGED_CELLS",
					"Merged cells detected. Please unmerge all cells before uploading.");
		}

		if (lowerError.contains("formula")) {
			return createGeneralError("FORMULA_DETECTED",
					"Excel formulas detected. Please convert formulas to values before uploading.");
		}

		if (lowerError.contains("protected") || lowerError.contains("password")) {
			return createGeneralError("PROTECTED_FILE",
					"File is password protected or contains protected sheets. Please remove protection.");
		}

		if (lowerError.contains("corrupt") || lowerError.contains("damaged")) {
			return createGeneralError("CORRUPTED_FILE",
					"File appears to be corrupted. Please try with a different file.");
		}

		if (lowerError.contains("empty row") || lowerError.contains("blank row")) {
			return createGeneralError("EMPTY_ROW", "Empty or blank row detected. Please remove empty rows from Excel.");
		}

		if (lowerError.contains("header") || lowerError.contains("column header")) {
			return createGeneralError("INVALID_HEADERS",
					"Invalid or missing column headers. Please check the Excel template.");
		}

		if (lowerError.contains("data type") || lowerError.contains("type mismatch")) {
			return createGeneralError("DATA_TYPE_MISMATCH",
					"Data type mismatch: " + extractRelevantPart(originalError));
		}

		return null; // Couldn't identify specific field
	}

	// Utility methods
	public static String createFieldError(String fieldName, String errorCode, String message) {
		return String.format("FIELD:%s:%s:%s", fieldName, errorCode, message);
	}

	public static String createGeneralError(String errorCode, String message) {
		return String.format("GENERAL:%s:%s", errorCode, message);
	}

	private String extractRelevantPart(String error) {
		// Extract the most relevant part of the error message
		if (error.length() > 100) {
			return error.substring(0, 97) + "...";
		}
		return error;
	}

	public static List<String> validateInputParameters(MultipartFile file, String academyId) {
		List<String> errors = new ArrayList<>();
		if (file == null || file.isEmpty()) {
			errors.add("File is required and cannot be empty");
		}
		if (academyId == null || academyId.trim().isEmpty()) {
			errors.add("Academy ID is required");
		}
		return errors;
	}

	// Response and error handling methods
	public static ServiceResponse createErrorResponse(String message, HttpStatus status) {
		Map<String, Object> errorBody = new HashMap<>();
		errorBody.put("success", false);
		errorBody.put("error", message);
		errorBody.put("timestamp", Instant.now().toString());
		errorBody.put("successCount", 0);
		errorBody.put("errorCount", 0);
		errorBody.put("totalProcessed", 0);
		return ResponseBuilder.error(errorBody, status);
	}

	public static ServiceResponse handleEmptyParseResult(List<ParsedRow> parsedRows) {
		Map<String, Object> errorBody = new HashMap<>();
		errorBody.put("success", false);
		errorBody.put("successCount", 0);
		errorBody.put("errorCount", 0);
		errorBody.put("totalProcessed", 0);
		errorBody.put("timestamp", Instant.now().toString());

		if (parsedRows == null || parsedRows.isEmpty()) {
			log.error("Excel parsing returned null");
			errorBody.put("error", "Failed to parse Excel file - invalid or empty file");
		}

		return ResponseBuilder.error(errorBody, HttpStatus.BAD_REQUEST);
	}

}
