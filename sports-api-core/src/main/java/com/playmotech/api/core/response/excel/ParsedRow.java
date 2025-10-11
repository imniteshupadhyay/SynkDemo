package com.playmotech.api.core.response.excel;

import java.util.ArrayList;
import java.util.List;

import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.ProgramPlayerEnrollmentDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UserProfileDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ParsedRow {
	private int rowNumber;
	private List<String> originalData;
	private UserProfileDto userDto;
	private CreateCourseDto courseDto;
	private TraineeCourseEnrollmentDto enrollmentDto;
	private ProgramPlayerEnrollmentDto playerEnrollmentDto;
	private ProgramDto program;
	private CoachDto coach;
	private PlayerDto player;
	private List<String> parsingErrors;

	public boolean hasParsingErrors() {
		return parsingErrors != null && !parsingErrors.isEmpty();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ProgramDto {
		private String programName;
		private String programId;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class CoachDto {
		private String coachName;
		private String coachId;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class PlayerDto {
		private String playerName;
		private String playerId;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class FieldError {
		private String field;
		private String message;
		private String value;
		private ErrorType type;

	}

	public enum ErrorType {
		REQUIRED_FIELD_MISSING, INVALID_FORMAT, DUPLICATE_IN_EXCEL, DUPLICATE_IN_DATABASE, BUSINESS_RULE_VIOLATION,
		DATA_TOO_LONG, INVALID_ENUM_VALUE, PARSING_ERROR
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class EnhancedRowError {
		private int rowNumber;
		private List<FieldError> fieldErrors;
		private List<String> generalErrors;
		private String status; // "ERROR", "WARNING"

		public EnhancedRowError(int rowNumber) {
			this.rowNumber = rowNumber;
			this.fieldErrors = new ArrayList<>();
			this.generalErrors = new ArrayList<>();
			this.status = "ERROR";
		}

		public void addFieldError(String field, String message, String value, ErrorType type) {
			fieldErrors.add(new FieldError(field, message, value, type));
		}

		public void addGeneralError(String message) {
			generalErrors.add(message);
		}

		public boolean hasErrors() {
			return !fieldErrors.isEmpty() || !generalErrors.isEmpty();
		}

	}
}