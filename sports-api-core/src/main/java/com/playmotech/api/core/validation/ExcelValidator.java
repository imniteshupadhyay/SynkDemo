package com.playmotech.api.core.validation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.playmotech.api.core.constants.DayOfWeek;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.ScheduleDto;
import com.playmotech.api.core.dto.UserProfileDto;

public class ExcelValidator {

	public static List<String> validateUserProfile(UserProfileDto dto, int rowIndex) {
		List<String> errors = new ArrayList<>();

		if (dto.getUserType() == null) {
			errors.add("FIELD:User Type:REQUIRED:User type is required");
			return errors;
		}

		switch (dto.getUserType()) {
			case PLAYER -> errors.addAll(validatePlayerProfile(dto, rowIndex));
			case COACH -> errors.addAll(validateCoachProfile(dto, rowIndex));
			default -> errors.add("FIELD:User Type:INVALID_VALUE:Unsupported user type");
		}

		return errors;
	}

	private static List<String> validateCoachProfile(UserProfileDto dto, int rowIndex) {
		List<String> errors = new ArrayList<>();

		// Format and validate display name
		String formattedName = formatDisplayName(dto.getDisplayName());
		dto.setDisplayName(formattedName);

		if (isEmpty(formattedName)) {
			errors.add("FIELD:Name:REQUIRED:Name is required");
		} else if (formattedName.length() > 100) {
			errors.add("FIELD:Name:TOO_LONG:Name cannot exceed 100 characters");
		}

		if (isEmpty(dto.getDesignation())) {
			errors.add("FIELD:Designation:REQUIRED:Designation is required");
		} else if (dto.getDesignation().length() > 50) {
			errors.add("FIELD:Designation:TOO_LONG:Designation cannot exceed 50 characters");
		}

		if (!isValidPhone(dto.getPhoneNumber())) {
			if (isEmpty(dto.getPhoneNumber())) {
				errors.add("FIELD:Phone Number:REQUIRED:Phone number is required");
			} else {
				errors.add(
						"FIELD:Phone Number:INVALID_FORMAT:Phone number must be exactly 12 digits including country code (e.g., 919876543210)");
			}
		}

		// Handle email validation - set to null if invalid but not empty
		if (!isEmpty(dto.getEmailId()) && !isValidEmail(dto.getEmailId())) {
			errors.add("FIELD:Email:INVALID_FORMAT:Please enter a valid email address (e.g., user@example.com)");
			dto.setEmailId(null); // Set to null only if provided but invalid
		} else if (dto.getEmailId() != null && dto.getEmailId().length() > 100) {
			errors.add("FIELD:Email:TOO_LONG:Email address cannot exceed 100 characters");
		}

		if (!isValidEnumValue(dto.getGender().toString().toUpperCase(), Gender.class)) {
			errors.add("FIELD:Gender:INVALID_VALUE:Gender must be valid");
		}

		if (!isValidEnumValue(dto.getRole().toString().toUpperCase(), Role.class)) {
			errors.add("FIELD:Role:INVALID_VALUE:Invalid role specified");
		}

		// Experience validation
		if (dto.getExperienceInMonths() != null) {
			if (dto.getExperienceInMonths() < 0) {
				errors.add("FIELD:Experience:INVALID_VALUE:Experience cannot be negative");
			} else if (dto.getExperienceInMonths() > 600) {
				errors.add("FIELD:Experience:INVALID_VALUE:Experience cannot exceed 600 months (50 years)");
			}
		}

		// Optional field validations with field names
		if (dto.getPincode() != null && !dto.getPincode().isBlank() && !dto.getPincode().matches("\\d{6}")) {
			errors.add("FIELD:Pincode:INVALID_FORMAT:Pincode must be exactly 6 digits");
		}

		if (dto.getCity() != null && dto.getCity().length() > 50) {
			errors.add("FIELD:City:TOO_LONG:City name cannot exceed 50 characters");
		}

		if (dto.getState() != null && dto.getState().length() > 50) {
			errors.add("FIELD:State:TOO_LONG:State name cannot exceed 50 characters");
		}

		if (dto.getAddressLine1() != null && dto.getAddressLine1().length() > 200) {
			errors.add("FIELD:Address Line 1:TOO_LONG:Address Line 1 cannot exceed 200 characters");
		}

		if (dto.getAddressLine2() != null && dto.getAddressLine2().length() > 200) {
			errors.add("FIELD:Address Line 2:TOO_LONG:Address Line 2 cannot exceed 200 characters");
		}

		return errors;
	}

	private static List<String> validatePlayerProfile(UserProfileDto dto, int rowIndex) {
		List<String> errors = new ArrayList<>();

		// Format and validate display name
		String formattedName = formatDisplayName(dto.getDisplayName());
		dto.setDisplayName(formattedName);

		if (isEmpty(formattedName)) {
			errors.add("FIELD:Name:REQUIRED:Name is required");
		} else if (formattedName.length() > 100) {
			errors.add("FIELD:Name:TOO_LONG:Name cannot exceed 100 characters");
		}

		if (!isValidPhone(dto.getPhoneNumber())) {
			if (isEmpty(dto.getPhoneNumber())) {
				errors.add("FIELD:Phone Number:REQUIRED:Phone number is required");
			} else {
				errors.add(
						"FIELD:Phone Number:INVALID_FORMAT:Phone number must be exactly 12 digits including country code (e.g., 919876543210)");
			}
		}

		// Email validation
		if (!isEmpty(dto.getEmailId()) && !isValidEmail(dto.getEmailId())) {
			errors.add("FIELD:Email:INVALID_FORMAT:Please enter a valid email address (e.g., user@example.com)");
			dto.setEmailId(null);
		} else if (dto.getEmailId() != null && dto.getEmailId().length() > 100) {
			errors.add("FIELD:Email:TOO_LONG:Email address cannot exceed 100 characters");
		}

		// Gender validation
		if (!isValidEnumValue(dto.getGender().toString().toUpperCase(), Gender.class)) {
			errors.add("FIELD:Gender:INVALID_VALUE:Gender must be valid");
		}

		if (!isValidEnumValue(dto.getRole().toString().toUpperCase(), Role.class)) {
			errors.add("FIELD:Role:INVALID_VALUE:Invalid role specified");
		}

		// Date of Birth validation
		validateAndFormatDateOfBirth(dto, rowIndex, errors);

		// Optional field validations
		if (dto.getPincode() != null && !dto.getPincode().isBlank() && !dto.getPincode().matches("\\d{6}")) {
			errors.add("FIELD:Pincode:INVALID_FORMAT:Pincode must be exactly 6 digits");
		}

		if (dto.getCity() != null && dto.getCity().length() > 50) {
			errors.add("FIELD:City:TOO_LONG:City name cannot exceed 50 characters");
		}

		if (dto.getState() != null && dto.getState().length() > 50) {
			errors.add("FIELD:State:TOO_LONG:State name cannot exceed 50 characters");
		}

		if (dto.getAddressLine1() != null && dto.getAddressLine1().length() > 200) {
			errors.add("FIELD:Address Line 1:TOO_LONG:Address Line 1 cannot exceed 200 characters");
		}

		if (dto.getAddressLine2() != null && dto.getAddressLine2().length() > 200) {
			errors.add("FIELD:Address Line 2:TOO_LONG:Address Line 2 cannot exceed 200 characters");
		}

		return errors;
	}

	/**
	 * Formats display name by: - Trimming leading/trailing spaces - Removing
	 * multiple consecutive spaces - Capitalizing first letter of each word - Making
	 * other letters lowercase
	 */
	private static String formatDisplayName(String displayName) {
		if (displayName == null || displayName.trim().isEmpty()) {
			return null;
		}

		// Remove leading/trailing spaces and replace multiple spaces with single space
		String cleaned = displayName.trim().replaceAll("\\s+", " ");

		// Split by spaces and capitalize each word
		String[] words = cleaned.split(" ");
		StringBuilder formatted = new StringBuilder();

		for (int i = 0; i < words.length; i++) {
			if (words[i].length() > 0) {
				// Capitalize first letter, lowercase the rest
				String word = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
				formatted.append(word);

				if (i < words.length - 1) {
					formatted.append(" ");
				}
			}
		}

		return formatted.toString();
	}

	/**
	 * Validates and formats date of birth
	 */
	private static void validateAndFormatDateOfBirth(UserProfileDto dto, int rowIndex, List<String> errors) {
		if (isEmpty(dto.getDob())) {
			// Set default date if empty
			dto.setDob("01-01-1981");
			return;
		}

		String dob = dto.getDob().trim();

		// List of supported date formats
		List<String> dateFormats = Arrays.asList("d/M/yyyy", "M/d/yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "d-M-yyyy",
				"M-d-yyyy", "dd-MM-yyyy", "MM-dd-yyyy", "d.M.yyyy", "M.d.yyyy", "dd.MM.yyyy", "MM.dd.yyyy",
				"yyyy-MM-dd", "yyyy/MM/dd", "M/dd/yyyy", "MM/d/yyyy", "d/MM/yyyy", "dd/M/yyyy");

		boolean validDate = false;

		for (String format : dateFormats) {
			try {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
				LocalDate parsedDate = LocalDate.parse(dob, formatter);

				LocalDate now = LocalDate.now();

				if (parsedDate.isAfter(now)) {
					errors.add("FIELD:Date of Birth:INVALID_VALUE:Date of birth cannot be in the future");
					return;
				}

				if (parsedDate.isBefore(now.minusYears(120))) {
					errors.add("FIELD:Date of Birth:INVALID_VALUE:Date of birth seems too far in the past");
					return;
				}

				// Format to standard format
				dto.setDob(parsedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
				validDate = true;
				break;

			} catch (DateTimeParseException e) {
				// try next format
			}
		}

		if (!validDate) {
			errors.add(
					"FIELD:Date of Birth:INVALID_FORMAT:Supported formats include dd-MM-yyyy, yyyy-MM-dd, etc.");
		}
	}

	/**
	 * Helper method to check if string is empty or null
	 */
	private static boolean isEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}

	// Comprehensive phone validation method with more formats and debug
	private static boolean isValidPhone(String phone) {
		// Debug: print the raw input
		System.out.println(
				"Phone number received: '" + phone + "', length: " + (phone == null ? "null" : phone.length()));

		// Check for null
		if (phone == null) {
			return false;
		}

		// Convert to string and handle Excel's potential double/numeric conversion
		String phoneStr = String.valueOf(phone).trim();

		// Handle Excel potentially converting to scientific notation like 9.87451E9
		if (phoneStr.contains("E")) {
			try {
				// Convert from scientific notation
				double phoneDouble = Double.parseDouble(phoneStr);
				phoneStr = String.format("%.0f", phoneDouble);
				System.out.println("Converted from scientific: " + phoneStr);
			} catch (NumberFormatException e) {
				// Not numeric, keep as is
				System.out.println("Failed to parse as number: " + phoneStr);
			}
		}

		// Clean the phone string - remove all non-digit characters
		String cleanedPhone = phoneStr.replaceAll("[^\\d]", "");
		System.out.println("Cleaned phone: '" + cleanedPhone + "', length: " + cleanedPhone.length());

		// Handle country code case (with 91 prefix)
		if (cleanedPhone.startsWith("91") && cleanedPhone.length() == 12) {
			return true;
		}
		// Handle 10-digit phone number without country code
		else if (cleanedPhone.length() == 10) {
			return true;
		}

		return false;
	}

	private static boolean isValidEmail(String email) {
		return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
	}

	private static <T extends Enum<T>> boolean isValidEnumValue(String value, Class<T> enumClass) {
		if (value == null) {
			return false;
		}
		try {
			Enum.valueOf(enumClass, value);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static List<String> validateCourse(CreateCourseDto dto, int rowIndex) {
		List<String> errors = new ArrayList<>();

		// Required fields validation
		if (dto.getTitle() == null || dto.getTitle().isBlank()) {
			errors.add("Row " + rowIndex + ": Title is required");
		}
		if (dto.getSport() == null) {
			errors.add("Row " + rowIndex + ": Sport is required");
		}
		if (dto.getLevel() == null) {
			errors.add("Row " + rowIndex + ": Skill Level is required");
		}
		if (dto.getTotalMaxTrainees() == null || dto.getTotalMaxTrainees() <= 0) {
			errors.add("Row " + rowIndex + ": Total Max Trainees must be a positive number");
		}
		if (dto.getVisibility() == null) {
			errors.add("Row " + rowIndex + ": Visibility is required");
		}
		if (dto.getRegistrationFee() == null || dto.getRegistrationFee() < 0) {
			errors.add("Row " + rowIndex + ": Registration Fee must be non-negative");
		}

		// Schedule validation
		errors.addAll(validateScheduleDatesAndTimes(dto.getSchedule(), rowIndex));

		// Validate DaysOfWeek based on ScheduleType
		ScheduleType scheduleType = dto.getSchedule().getType();
		List<DayOfWeek> weekdays = dto.getSchedule().getWeekdays();

		if (scheduleType == ScheduleType.WEEKENDS) {
			if (weekdays == null || weekdays.isEmpty()) {
				errors.add("Row " + rowIndex + ": Days of week are required for WEEKENDS schedule");
			} else {
				for (DayOfWeek day : weekdays) {
					if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
						errors.add("Row " + rowIndex + ": WEEKENDS schedule can only include Saturday/Sunday");
						break;
					}
				}
			}
		} else if (scheduleType == ScheduleType.WEEKDAYS) {
			if (weekdays == null || weekdays.isEmpty()) {
				errors.add("Row " + rowIndex + ": Days of week are required for WEEKDAYS schedule");
			} else {
				for (DayOfWeek day : weekdays) {
					if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
						errors.add("Row " + rowIndex + ": WEEKDAYS schedule cannot include weekends");
						break;
					}
				}
			}
		} else if (scheduleType == ScheduleType.CUSTOM) {
			if (weekdays == null || weekdays.isEmpty()) {
				errors.add("Row " + rowIndex + ": Days of week are required for CUSTOM schedule");
			}
		}

		// Payment options validation
		if (dto.getPaymentOptions() != null) {
			dto.getPaymentOptions().forEach((type, amount) -> {
				if (amount <= 0) {
					errors.add("Row " + rowIndex + ": Payment amount for " + type + " must be positive");
				}
			});
		}

		return errors;
	}

	public static List<String> validateScheduleDatesAndTimes(ScheduleDto schedule, int rowIndex) {
		List<String> errors = new ArrayList<>();

		DateTimeFormatter inputDateFormat = new DateTimeFormatterBuilder().parseCaseInsensitive()
				.appendPattern("dd-MM-yyyy").toFormatter();

		DateTimeFormatter inputTimeFormat = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("HH:mm")
				.toFormatter();
		DateTimeFormatter outputDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter outputTimeFormat = DateTimeFormatter.ofPattern("hh:mm a");

		// Start Date
		String startDateStr = schedule.getStartDate();

		startDateStr = sanitize(schedule.getStartDate());
		if (!isBlank(startDateStr)) {
			try {
				LocalDate date = LocalDate.parse(startDateStr.trim(), inputDateFormat);
				schedule.setStartDate(date.format(outputDateFormat));
			} catch (DateTimeParseException e) {
				errors.add("Row " + rowIndex + ": Invalid Start Date format (expected DD-MM-YYYY): " + startDateStr);
			}
		}

		// End Date
		String endDateStr = schedule.getEndDate();

		endDateStr = sanitize(schedule.getEndDate());
		if (!isBlank(endDateStr)) {
			try {
				LocalDate date = LocalDate.parse(endDateStr.trim(), inputDateFormat);
				schedule.setEndDate(date.format(outputDateFormat));
			} catch (DateTimeParseException e) {
				errors.add("Row " + rowIndex + ": Invalid End Date format (expected DD-MM-YYYY): " + endDateStr);
			}
		}

		// Start Time
		String startTimeStr = schedule.getStartTime();
		// End Time
		String endTimeStr = schedule.getEndTime();

		// In validateScheduleDatesAndTimes():
		if ("31/12/1899".equals(startTimeStr) || "31/12/1899".equals(endTimeStr)) {
			errors.add("Row " + rowIndex + ": Time field is empty/invalid");
		}

		if (!isBlank(startTimeStr)) {
			try {
				LocalTime time = LocalTime.parse(startTimeStr.trim(), inputTimeFormat);
				schedule.setStartTime(time.format(outputTimeFormat));
			} catch (DateTimeParseException e) {
				errors.add("Row " + rowIndex + ": Invalid Start Time format (expected HH:MM): " + startTimeStr);
			}
		}

		if (!isBlank(endTimeStr)) {
			try {
				LocalTime time = LocalTime.parse(endTimeStr.trim(), inputTimeFormat);
				schedule.setEndTime(time.format(outputTimeFormat));
			} catch (DateTimeParseException e) {
				errors.add("Row " + rowIndex + ": Invalid End Time format (expected HH:MM): " + endTimeStr);
			}
		}

		System.out.println("Raw Start Time: '" + startTimeStr + "'");

		startTimeStr = sanitize(schedule.getStartTime());

		System.out.println("Raw Start Time: '" + startTimeStr + "'");

		return errors;
	}

	public static String sanitize(String input) {
		if (input == null) {
			return null;
		}
		// Replace non-breaking spaces, slashes, and trim
		return input.replace('\u00A0', ' ').replace("/", "-") // Handle slashes in dates
				.trim();
	}

	// Usage:

	private static boolean isBlank(String s) {
		return s == null || s.replaceAll("\\s+", "").isEmpty();
	}
}
