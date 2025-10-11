package com.playmotech.api.core.utils.excel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.DayOfWeek;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.ScheduleType;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.ProgramPlayerEnrollmentDto;
import com.playmotech.api.core.dto.ScheduleDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.response.excel.ParsedRow;

@Component
public class ExcelUserUploadUtil {

    // Template headers for coach bulk upload
    public static final String[] COACH_TEMPLATE_HEADERS = { "Name", "Phone Number", "Designation", "Email", "Gender",
            "Role", "Experience (Months)", "Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country" };

    public static final String[] COACH_TEMPLATE_HEADERS_WITH_ID = { "Name", "Phone Number", "Designation", "Email",
            "Gender", "Role", "Experience (Months)", "Address Line 1", "Address Line 2", "Pincode", "City", "State",
            "Country", "ID" };

    public static final String[] PLAYER_HEADERS = { "Full Name", "Phone", "Gender", "Date of Birth", "Email",
            "Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country" };

    public static final String[] PLAYER_HEADERS_WITH_ID = { "Full Name", "Phone", "Gender", "Date of Birth", "Email",
            "Address Line 1", "Address Line 2", "Pincode", "City", "State", "Country", "ID" };

    public static final String[] PROGRAM_HEADERS = { "Title*", "Description", "Sport*", "Skill Level*", "Age Category*",
            "Total Max Trainees*", "Visibility*", "Registration Fee*", "Currency*", "Schedule Type*", "Days of Week",
            "Start Date* (e.g., 05-Jun-2025)", "End Date* (e.g., 15-Jul-2025)", "Session Start Time* (HH:MM)",
            "Session End Time* (HH:MM)", "Payment Schedule Types* (Comma-separated)",
            "Payment Amounts* (Comma-separated)" };

    public static final String[] PROGRAM_PLAYER_ENROLLMENT_HEADERS = { "Full Name", "Phone Number", "Fee Amount",
            "Payment Schedule", "Joining Date (e.g., 05-Jun-2025)", "Next Due Date (e.g., 15-Jul-2025)" };

    // DataFormatter to get the actual formatted value as displayed in Excel
    private static final DataFormatter dataFormatter = new DataFormatter();

    // Regex pattern for accepted date formats
    // Group 1: Day (1-2 digits)
    // Group 2: Month (3-9 letters for Jan-September)
    // Group 3: Year (4 digits)
    private static final Pattern ACCEPTED_DATE_PATTERN = Pattern
            .compile("^(\\d{1,2})[-/ ]([A-Za-z]{3,9})[-/ ](\\d{4})$");

    /**
     * Checks if a string matches the accepted date format patterns
     * <p>
     * Accepted patterns: - dd-MMM-yyyy (e.g., 06-Jul-2025, 1-Jan-2024) -
     * dd-MMMM-yyyy (e.g., 06-August-2025, 31-December-2023) - Same with / or space
     * separators
     * <p>
     * Rejected patterns: - Numeric months (06-07-2025, 31-01-2024) - 2-digit years
     * (06-Jul-25) - Any other format
     */
    private static boolean isAcceptedDateFormat(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return ACCEPTED_DATE_PATTERN.matcher(value).matches();
    }

    /**
     * Main method to extract date value from Excel cell Returns null if the date
     * doesn't match accepted formats
     */
    public static String dateValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                String raw = cell.getStringCellValue().trim();
                return isAcceptedDateFormat(raw) ? raw : null;

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Get the actual formatted value as it appears in Excel
                    String cellFormattedValue = dataFormatter.formatCellValue(cell).trim();
                    return isAcceptedDateFormat(cellFormattedValue) ? cellFormattedValue : null;
                } else {
                    // Not date-formatted numeric - try as string fallback
                    String fallbackNumeric = String.valueOf((long) cell.getNumericCellValue());
                    return isAcceptedDateFormat(fallbackNumeric) ? fallbackNumeric : null;
                }

            case FORMULA:
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        String formulaRaw = cell.getStringCellValue().trim();
                        return isAcceptedDateFormat(formulaRaw) ? formulaRaw : null;

                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            // Get the actual formatted value as it appears in Excel
                            String cellFormattedValue = dataFormatter.formatCellValue(cell).trim();
                            return isAcceptedDateFormat(cellFormattedValue) ? cellFormattedValue : null;
                        } else {
                            String fallbackFormulaNumeric = String.valueOf((long) cell.getNumericCellValue());
                            return isAcceptedDateFormat(fallbackFormulaNumeric) ? fallbackFormulaNumeric : null;
                        }

                    default:
                        return null;
                }

            default:
                return null;
        }
    }

    /**
     * Utility method for testing - validates a string directly
     */
    public static boolean isValidDateString(String dateString) {
        return isAcceptedDateFormat(dateString);
    }

    /**
     * Demo method showing accepted vs rejected examples
     */
    public static void demonstrateValidation() {
        System.out.println("=== STRICT DATE VALIDATION DEMO ===\n");

        String[] testCases = {
                // ACCEPTED EXAMPLES
                "31-Jan-2024", // ✅ Valid format
                "15/December/2023", // ✅ Valid format with /
                "1 Jan 2024", // ✅ Valid format with space
                "06-Jul-2025", // ✅ Valid format
                "06/August/2025", // ✅ Valid format
                "9-Sep-2023", // ✅ Single digit day
                "25 November 2024", // ✅ Full month name

                // REJECTED EXAMPLES - Excel formatted dates that don't match
                "06-07-25", // ❌ 2-digit year (Excel might format like this)
                "06-08-25", // ❌ 2-digit year (Excel might format like this)
                "06-07-2025", // ❌ Numeric month (Excel might format like this)
                "31-01-2024", // ❌ Numeric month (Excel might format like this)
                "15/12/2023", // ❌ Numeric month (Excel might format like this)
                "7/6/2025", // ❌ Numeric month (Excel might format like this)
                "2025-07-06", // ❌ Wrong order (Excel might format like this)
                "Jul-06-2025", // ❌ Wrong order
                "06-July-25", // ❌ 2-digit year
                "6/7/2025", // ❌ Numeric month
                "", // ❌ Empty
                "invalid", // ❌ Invalid format
                "32-Jan-2024", // ❌ Invalid day (but format check passes)
                "06-13-2025" // ❌ Numeric month
        };

        for (String testCase : testCases) {
            boolean isValid = isValidDateString(testCase);
            String status = isValid ? "✅ ACCEPTED" : "❌ REJECTED";
            System.out.printf("%-20s → %s%n", "'" + testCase + "'", status);
        }

        System.out.println("\n=== CRITICAL BEHAVIOR ===");
        System.out.println("• Uses DataFormatter.formatCellValue() to get actual Excel display");
        System.out.println("• Does NOT format dates - uses exact cell formatted value");
        System.out.println("• If Excel shows '06-07-25' → REJECTED (2-digit year)");
        System.out.println("• If Excel shows '06-07-2025' → REJECTED (numeric month)");
        System.out.println("• If Excel shows '6/7/2025' → REJECTED (numeric month)");
        System.out.println("• If Excel shows '06-Jul-2025' → ACCEPTED (correct format)");
        System.out.println("• Month MUST be text (Jan, Feb, July, August, etc.)");
        System.out.println("• Year MUST be 4 digits");
        System.out.println("• Day can be 1 or 2 digits");
        System.out.println("• Separators: - / or space");
        System.out.println("• Everything else returns null");
    }

    /**
     * Main method for testing
     */
    // public static void main(String[] args) {
    // demonstrateValidation();
    // }

    // Updated parseDate method - with strict format validation before parsing
    private static LocalDate parseNewDate(String dateStr) throws DateTimeParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new DateTimeParseException("Date string is null or empty", dateStr, 0);
        }

        String trimmed = dateStr.trim();

        // Pre-validate the format before attempting to parse
        if (!isValidDateFormat(trimmed)) {
            throw new DateTimeParseException("Invalid date format: " + trimmed
                    + ". Only supported formats: dd-MMM-YYYY, dd/MMM/YYYY, dd MMM YYYY (e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024) or dd-MMMM-YYYY, dd/MMMM/YYYY, dd MMMM YYYY (e.g., 15-December-2023, 15/December/2023, 15 December 2023)",
                    trimmed, 0);
        }

        // Support both formats with multiple separators:
        // 1. dd-MMM-YYYY, dd/MMM/YYYY, dd MMM YYYY (e.g., 31-Jan-2024, 31/Jan/2024, 31
        // Jan 2024)
        // 2. dd-MMMM-YYYY, dd/MMMM/YYYY, dd MMMM YYYY (e.g., 15-December-2023,
        // 15/December/2023, 15 December 2023)

        List<DateTimeFormatter> formatters = Arrays.asList(
                // dd-MMM-YYYY patterns with different separators
                DateTimeFormatter.ofPattern("dd-MMM-yyyy"), // 31-Jan-2024
                DateTimeFormatter.ofPattern("d-MMM-yyyy"), // 1-Jan-2024
                DateTimeFormatter.ofPattern("dd/MMM/yyyy"), // 31/Jan/2024
                DateTimeFormatter.ofPattern("d/MMM/yyyy"), // 1/Jan/2024
                DateTimeFormatter.ofPattern("dd MMM yyyy"), // 31 Jan 2024
                DateTimeFormatter.ofPattern("d MMM yyyy"), // 1 Jan 2024

                // dd-MMMM-YYYY patterns with different separators
                DateTimeFormatter.ofPattern("dd-MMMM-yyyy"), // 15-December-2023
                DateTimeFormatter.ofPattern("d-MMMM-yyyy"), // 1-December-2023
                DateTimeFormatter.ofPattern("dd/MMMM/yyyy"), // 15/December/2023
                DateTimeFormatter.ofPattern("d/MMMM/yyyy"), // 1/December/2023
                DateTimeFormatter.ofPattern("dd MMMM yyyy"), // 15 December 2023
                DateTimeFormatter.ofPattern("d MMMM yyyy") // 1 December 2023
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        throw new DateTimeParseException("Unable to parse date: " + trimmed
                + ". Only supported formats: dd-MMM-YYYY, dd/MMM/YYYY, dd MMM YYYY (e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024) or dd-MMMM-YYYY, dd/MMMM/YYYY, dd MMMM YYYY (e.g., 15-December-2023, 15/December/2023, 15 December 2023)",
                trimmed, 0);
    }

    // Helper method to validate date format before parsing
    private static boolean isValidDateFormat(String dateStr) {
        // Check if the string matches our expected patterns
        String[] parts;

        // Try different separators
        if (dateStr.contains("-")) {
            parts = dateStr.split("-");
        } else if (dateStr.contains("/")) {
            parts = dateStr.split("/");
        } else if (dateStr.contains(" ")) {
            parts = dateStr.trim().split("\\s+");
        } else {
            return false; // No valid separator found
        }

        // Must have exactly 3 parts: day, month, year
        if (parts.length != 3) {
            return false;
        }

        String day = parts[0].trim();
        String month = parts[1].trim();
        String year = parts[2].trim();

        // Validate day (1-31, 1-2 digits)
        if (!day.matches("^\\d{1,2}$")) {
            return false;
        }

        // Validate year (exactly 4 digits)
        if (!year.matches("^\\d{4}$")) {
            return false;
        }

        // Validate month (must be text, not numeric)
        if (month.matches("^\\d+$")) {
            return false; // Numeric month not allowed
        }

        // Check if month is valid short month (MMM) or full month (MMMM)
        String[] shortMonths = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        String[] fullMonths = { "January", "February", "March", "April", "May", "June", "July", "August", "September",
                "October", "November", "December" };

        boolean isShortMonth = Arrays.stream(shortMonths).anyMatch(m -> m.equalsIgnoreCase(month));
        boolean isFullMonth = Arrays.stream(fullMonths).anyMatch(m -> m.equalsIgnoreCase(month));

        return isShortMonth || isFullMonth;
    }

    // Updated Excel parser method with improved date parsing error messages
    public static List<ParsedRow> parseProgramPlayerEnrollmentExcelWithErrors(MultipartFile file) throws IOException {
        List<ParsedRow> parsedRows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Validate header row
            Row header = sheet.getRow(0);
            for (int i = 0; i < PROGRAM_PLAYER_ENROLLMENT_HEADERS.length; i++) {
                Cell cell = header.getCell(i);
                String val = cell != null ? cell.getStringCellValue().trim() : null;
                if (val == null || !PROGRAM_PLAYER_ENROLLMENT_HEADERS[i].equalsIgnoreCase(val)) {
                    throw new IOException("Invalid header at column " + (i + 1) + ". Expected '"
                            + PROGRAM_PLAYER_ENROLLMENT_HEADERS[i] + "' but found '" + val + "'.");
                }
            }

            for (int idx = 1; idx <= sheet.getLastRowNum(); idx++) {
                Row row = sheet.getRow(idx);
                if (row == null) {
                    continue;
                }

                // Skip row if both name and phone number are blank
                String name = value(row.getCell(0));
                String phone = value(row.getCell(1));
                if (!StringUtils.hasText(name) && !StringUtils.hasText(phone)) {
                    continue;
                }

                List<String> originalData = new ArrayList<>();
                List<String> parsingErrors = new ArrayList<>();
                ProgramPlayerEnrollmentDto enrollmentDto = new ProgramPlayerEnrollmentDto();

                // Full Name (Column 0)
                originalData.add(name);
                if (name == null || name.isBlank()) {
                    parsingErrors.add("FIELD:Name:REQUIRED:Name is mandatory and cannot be empty");
                } else if (name.length() > 100) {
                    parsingErrors.add("FIELD:Name:TOO_LONG:Name cannot exceed 100 characters");
                }
                enrollmentDto.setFullName(name);

                // Phone Number (Column 1)
                originalData.add(phone);
                if (phone == null || phone.isBlank()) {
                    parsingErrors.add("FIELD:Phone Number:REQUIRED:Phone number is mandatory");
                } else {
                    phone = phone.replaceAll("[^0-9]", ""); // Remove non-digits
                    if (phone.length() != 12) {
                        parsingErrors.add(
                                "FIELD:Phone Number:INVALID_FORMAT:Phone number must be exactly 12 digits including country code (e.g., 919876543210)");
                    } else if (!phone.startsWith("91")) {
                        parsingErrors
                                .add("FIELD:Phone Number:INVALID_FORMAT:Phone number must start with country code 91");
                    }
                }
                enrollmentDto.setPhoneNumber(phone);

                // Fee Amount (Column 2)
                String feeStr = value(row.getCell(2));
                originalData.add(feeStr);
                if (feeStr == null || feeStr.isBlank()) {
                    parsingErrors.add("FIELD:Fee Amount:REQUIRED:Fee amount is required");
                } else {
                    try {
                        Long fee = Long.parseLong(feeStr.trim());
                        if (fee < 0) {
                            parsingErrors.add("FIELD:Fee Amount:INVALID_VALUE:Fee amount cannot be negative");
                        }
                        enrollmentDto.setFeeAmount(fee);
                    } catch (NumberFormatException e) {
                        parsingErrors.add("FIELD:Fee Amount:INVALID_FORMAT:Invalid fee amount format: " + feeStr);
                    }
                }

                // Payment Schedule (Column 3)
                String paymentScheduleStr = value(row.getCell(3));
                originalData.add(paymentScheduleStr);
                if (paymentScheduleStr == null || paymentScheduleStr.isBlank()) {
                    parsingErrors.add("FIELD:Payment Schedule:REQUIRED:Payment Schedule is required");
                } else {
                    try {
                        PaymentSchedule paymentSchedule = PaymentSchedule
                                .valueOf(paymentScheduleStr.trim().toUpperCase());
                        enrollmentDto.setPaymentSchedule(paymentSchedule);
                    } catch (IllegalArgumentException e) {
                        parsingErrors.add(
                                "FIELD:Payment Schedule:INVALID_VALUE:Invalid Payment Schedule: " + paymentScheduleStr
                                        + ". Valid options are: " + Arrays.toString(PaymentSchedule.values()));
                    }
                }

                // Joining Date (Column 4) - Updated with multiple separator support
                String joiningDateStr = dateValue(row.getCell(4));
                if (joiningDateStr != null && !joiningDateStr.isBlank()) {
                    try {
                        LocalDate joiningDate = parseNewDate(joiningDateStr.trim());
                        enrollmentDto.setJoiningDate(joiningDate);
                        originalData.add(joiningDateStr); // Only add if valid
                    } catch (DateTimeParseException e) {
                        enrollmentDto.setJoiningDate(null);
                        originalData.add(null); // Bad value replaced with null
                        parsingErrors.add(
                                "FIELD:Joining Date:INVALID_FORMAT:Invalid joining date format. Only supported formats: (e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024, 15-December-2023, 15/December/2023, 15 December 2023)");
                    }
                } else {
                    originalData.add(null); // If blank, also put null
                    parsingErrors.add(
                            "FIELD:Joining Date:REQUIRED:Invalid joining date format. Only supported formats: (e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024, 15-December-2023, 15/December/2023, 15 December 2023)");
                }

                // Next Due Date (Column 5) - Updated with multiple separator support
                String dueDateStr = dateValue(row.getCell(5));
                if (dueDateStr != null && !dueDateStr.isBlank()) {
                    try {
                        LocalDate dueDate = parseNewDate(dueDateStr.trim());
                        enrollmentDto.setNextDueDate(dueDate);
                        originalData.add(dueDateStr);
                    } catch (DateTimeParseException e) {
                        enrollmentDto.setNextDueDate(null);
                        originalData.add(null);
                        parsingErrors.add(
                                "FIELD:Next Due Date:INVALID_FORMAT:Invalid due date format. Only supported formats: (e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024, 15-December-2023, 15/December/2023, 15 December 2023)");
                    }
                } else {
                    originalData.add(null);
                    parsingErrors.add(
                            "FIELD:Next Due Date:REQUIRED:Invalid next due date date format. Only supported formats: (e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024, 15-December-2023, 15/December/2023, 15 December 2023)");
                }

                parsedRows.add(new ParsedRow(idx + 1, originalData, null, null, null, enrollmentDto, null, null, null,
                        parsingErrors));
            }
        }
        return parsedRows;
    }

    // Updated parseDate method with required format elements
    private static LocalDate parseNewDate2(String dateStr) throws DateTimeParseException {
        // Required format elements:
        // 1. Month: Must be in MMM (Jan, Feb, Dec) or MMMM (January, February,
        // December) format
        // 2. Year: Must be in YYYY format (4 digits: 2024, 2023, etc.)
        // 3. Separators: Any separator (-, /, ., space) is allowed

        List<DateTimeFormatter> formatters = Arrays.asList(
                // dd-MMM-YYYY patterns
                DateTimeFormatter.ofPattern("dd-MMM-yyyy"), DateTimeFormatter.ofPattern("d-MMM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MMM/yyyy"), DateTimeFormatter.ofPattern("d/MMM/yyyy"),
                DateTimeFormatter.ofPattern("dd.MMM.yyyy"), DateTimeFormatter.ofPattern("d.MMM.yyyy"),
                DateTimeFormatter.ofPattern("dd MMM yyyy"), DateTimeFormatter.ofPattern("d MMM yyyy"),

                // dd-MMMM-YYYY patterns
                DateTimeFormatter.ofPattern("dd-MMMM-yyyy"), DateTimeFormatter.ofPattern("d-MMMM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MMMM/yyyy"), DateTimeFormatter.ofPattern("d/MMMM/yyyy"),
                DateTimeFormatter.ofPattern("dd.MMMM.yyyy"), DateTimeFormatter.ofPattern("d.MMMM.yyyy"),
                DateTimeFormatter.ofPattern("dd MMMM yyyy"), DateTimeFormatter.ofPattern("d MMMM yyyy"),

                // MMM dd YYYY patterns
                DateTimeFormatter.ofPattern("MMM dd yyyy"), DateTimeFormatter.ofPattern("MMM d yyyy"),
                DateTimeFormatter.ofPattern("MMM-dd-yyyy"), DateTimeFormatter.ofPattern("MMM-d-yyyy"),
                DateTimeFormatter.ofPattern("MMM/dd/yyyy"), DateTimeFormatter.ofPattern("MMM/d/yyyy"),
                DateTimeFormatter.ofPattern("MMM.dd.yyyy"), DateTimeFormatter.ofPattern("MMM.d.yyyy"),

                // MMMM dd YYYY patterns
                DateTimeFormatter.ofPattern("MMMM dd yyyy"), DateTimeFormatter.ofPattern("MMMM d yyyy"),
                DateTimeFormatter.ofPattern("MMMM-dd-yyyy"), DateTimeFormatter.ofPattern("MMMM-d-yyyy"),
                DateTimeFormatter.ofPattern("MMMM/dd/yyyy"), DateTimeFormatter.ofPattern("MMMM/d/yyyy"),
                DateTimeFormatter.ofPattern("MMMM.dd.yyyy"), DateTimeFormatter.ofPattern("MMMM.d.yyyy"),

                // MMMM dd, YYYY patterns (with comma)
                DateTimeFormatter.ofPattern("MMMM dd, yyyy"), DateTimeFormatter.ofPattern("MMMM d, yyyy"),
                DateTimeFormatter.ofPattern("MMM dd, yyyy"), DateTimeFormatter.ofPattern("MMM d, yyyy"),

                // YYYY-MMM-dd patterns
                DateTimeFormatter.ofPattern("yyyy-MMM-dd"), DateTimeFormatter.ofPattern("yyyy-MMM-d"),
                DateTimeFormatter.ofPattern("yyyy/MMM/dd"), DateTimeFormatter.ofPattern("yyyy/MMM/d"),
                DateTimeFormatter.ofPattern("yyyy.MMM.dd"), DateTimeFormatter.ofPattern("yyyy.MMM.d"),
                DateTimeFormatter.ofPattern("yyyy MMM dd"), DateTimeFormatter.ofPattern("yyyy MMM d"),

                // YYYY-MMMM-dd patterns
                DateTimeFormatter.ofPattern("yyyy-MMMM-dd"), DateTimeFormatter.ofPattern("yyyy-MMMM-d"),
                DateTimeFormatter.ofPattern("yyyy/MMMM/dd"), DateTimeFormatter.ofPattern("yyyy/MMMM/d"),
                DateTimeFormatter.ofPattern("yyyy.MMMM.dd"), DateTimeFormatter.ofPattern("yyyy.MMMM.d"),
                DateTimeFormatter.ofPattern("yyyy MMMM dd"), DateTimeFormatter.ofPattern("yyyy MMMM d"));

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        throw new DateTimeParseException(
                "Unable to parse date: " + dateStr
                        + ". Expected formats: dd-MMM-YYYY, dd/MMMM/YYYY, MMM dd YYYY, January 15, 2024, etc. "
                        + "Month must be in MMM (Jan, Feb) or MMMM (January, February) format. Year must be 4 digits.",
                dateStr, 0);
    }

    private static LocalDate parseDate(String dateStr) throws DateTimeParseException {
        List<DateTimeFormatter> formatters = Arrays.asList(DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        throw new DateTimeParseException("Unable to parse date: " + dateStr, dateStr, 0);
    }

    /**
     * Parses an Excel sheet of user profiles, capturing both row data and any
     * parsing errors.
     */
    public static List<ParsedRow> parseExcelWithErrors(MultipartFile file) throws IOException {
        List<ParsedRow> parsedRows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int idx = 1; idx <= sheet.getLastRowNum(); idx++) {
                Row row = sheet.getRow(idx);
                if (row == null) {
                    continue;
                }

                List<String> originalData = new ArrayList<>();
                List<String> parsingErrors = new ArrayList<>();

                String username = value(row.getCell(0));
                originalData.add(username);
                if (username == null || username.isBlank()) {
                    parsingErrors.add("Username is required");
                }

                String displayName = value(row.getCell(1));
                originalData.add(displayName);
                if (displayName == null || displayName.isBlank()) {
                    parsingErrors.add("Display name is required");
                }

                String phone = value(row.getCell(2));
                originalData.add(phone);
                if (phone == null || phone.isBlank()) {
                    parsingErrors.add("Phone number is required");
                }

                String email = value(row.getCell(3));
                originalData.add(email);
                if (email == null || email.isBlank()) {
                    parsingErrors.add("Email is required");
                }

                String genderStr = value(row.getCell(4));
                originalData.add(genderStr);
                Gender gender = null;
                if (genderStr != null && !genderStr.isBlank()) {
                    try {
                        gender = Gender.valueOf(genderStr.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        parsingErrors.add("Invalid gender: " + genderStr);
                    }
                }

                UserProfileDto dto = new UserProfileDto();
                dto.setUsername(username);
                dto.setDisplayName(displayName);
                dto.setPhoneNumber(phone);
                dto.setEmailId(email);
                dto.setGender(gender);

                parsedRows.add(
                        new ParsedRow(idx + 1, originalData, dto, null, null, null, null, null, null, parsingErrors));
            }
        }
        return parsedRows;
    }

    /**
     * Parses an Excel sheet of coach profiles against the COACH_TEMPLATE_HEADERS,
     * capturing parsing errors.
     */
    /**
     * Parses an Excel sheet of coach profiles, automatically detecting whether ID
     * column is present
     */
    public static List<ParsedRow> parseCoachExcelWithErrors(MultipartFile file) throws IOException {
        List<ParsedRow> parsedRows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Detect header format and validate
            Row header = sheet.getRow(0);
            boolean hasIdColumn = detectCoachHeaderFormat(header);
            String[] expectedHeaders = hasIdColumn ? COACH_TEMPLATE_HEADERS_WITH_ID : COACH_TEMPLATE_HEADERS;

            // Validate header row
            for (int i = 0; i < expectedHeaders.length; i++) {
                Cell cell = header.getCell(i);
                String val = cell != null ? cell.getStringCellValue().trim() : null;
                if (!expectedHeaders[i].equals(val)) {
                    throw new IOException("Invalid header at column " + (i + 1) + ". Expected '" + expectedHeaders[i]
                            + "' but found '" + val + "'.");
                }
            }

            // Parse rows with enhanced error tracking
            for (int idx = 1; idx <= sheet.getLastRowNum(); idx++) {
                Row row = sheet.getRow(idx);
                if (row == null)
                    continue;

                String name = value(row.getCell(0));
                String phone = value(row.getCell(1));
                if (!StringUtils.hasText(name) && !StringUtils.hasText(phone)) {
                    continue;
                }

                List<String> originalData = new ArrayList<>();
                List<String> parsingErrors = new ArrayList<>();
                UserProfileDto dto = new UserProfileDto();

                // Name validation
                originalData.add(name);
                if (name == null || name.isBlank()) {
                    parsingErrors.add("FIELD:Name:REQUIRED:Name is mandatory and cannot be empty");
                } else if (name.length() > 100) {
                    parsingErrors.add("FIELD:Name:TOO_LONG:Name cannot exceed 100 characters");
                }
                dto.setDisplayName(name);

                // Phone Number validation
                originalData.add(phone);
                if (phone == null || phone.isBlank()) {
                    parsingErrors.add("FIELD:Phone Number:REQUIRED:Phone number is mandatory");
                } else {
                    phone = phone.replaceAll("[^0-9]", ""); // Remove non-digits
                    if (phone.length() != 12) {
                        parsingErrors.add(
                                "FIELD:Phone Number:INVALID_FORMAT:Phone number must be exactly 12 digits including country code (e.g., 919876543210)");
                    } else if (!phone.startsWith("91")) {
                        parsingErrors
                                .add("FIELD:Phone Number:INVALID_FORMAT:Phone number must start with country code 91");
                    }
                }
                dto.setPhoneNumber(phone);

                // Designation validation
                String desig = value(row.getCell(2));
                originalData.add(desig);
                if (desig == null || desig.isBlank()) {
                    parsingErrors.add("FIELD:Designation:REQUIRED:Designation is mandatory");
                } else if (desig.length() > 50) {
                    parsingErrors.add("FIELD:Designation:TOO_LONG:Designation cannot exceed 50 characters");
                }
                dto.setDesignation(desig);

                // Email validation
                String email = value(row.getCell(3));
                originalData.add(email);
                if (email == null || email.isBlank()) {
                    parsingErrors.add("FIELD:Email:REQUIRED:Email address is mandatory");
                } else {
                    email = email.toLowerCase().trim();
                    if (!isValidEmailFormat(email)) {
                        parsingErrors.add(
                                "FIELD:Email:INVALID_FORMAT:Please enter a valid email address (e.g., user@example.com)");
                    } else if (email.length() > 100) {
                        parsingErrors.add("FIELD:Email:TOO_LONG:Email address cannot exceed 100 characters");
                    }
                }
                dto.setEmailId(email);

                // Gender validation
                String genderStr = value(row.getCell(4));
                originalData.add(genderStr);
                if (genderStr == null || genderStr.isBlank()) {
                    parsingErrors.add("FIELD:Gender:REQUIRED:Gender is mandatory");
                } else {
                    try {
                        dto.setGender(Gender.valueOf(genderStr.trim().toUpperCase()));
                    } catch (Exception e) {
                        parsingErrors.add("FIELD:Gender:INVALID_VALUE:Gender must be valid, found: " + genderStr);
                    }
                }

                // Role validation
                String roleStr = value(row.getCell(5));
                originalData.add(roleStr);
                if (roleStr == null || roleStr.isBlank()) {
                    parsingErrors.add("FIELD:Role:REQUIRED:Role is mandatory");
                } else {
                    try {
                        Role role = Role.valueOf(roleStr.trim().toUpperCase());
                        if (!isValidCoachRole(role)) {
                            parsingErrors.add(
                                    "FIELD:Role:INVALID_VALUE:Invalid role for coach. Allowed roles: COACH, HEAD_COACH, ASSISTANT_COACH");
                        }
                        dto.setRole(role);
                    } catch (Exception e) {
                        parsingErrors.add("FIELD:Role:INVALID_VALUE:Invalid role specified: " + roleStr);
                    }
                }

                // Experience validation
                String expStr = value(row.getCell(6));
                originalData.add(expStr);
                if (expStr == null || expStr.isBlank()) {
                    parsingErrors.add("FIELD:Experience:REQUIRED:Experience in months is mandatory");
                } else {
                    try {
                        int experience = Integer.parseInt(expStr.trim());
                        if (experience < 0) {
                            parsingErrors.add("FIELD:Experience:INVALID_VALUE:Experience cannot be negative");
                        } else if (experience > 600) { // 50 years max
                            parsingErrors.add(
                                    "FIELD:Experience:INVALID_VALUE:Experience cannot exceed 600 months (50 years)");
                        }
                        dto.setExperienceInMonths(experience);
                    } catch (NumberFormatException e) {
                        parsingErrors.add("FIELD:Experience:INVALID_FORMAT:Experience must be a valid number (months)");
                    }
                }

                // Address Line 1 validation
                String a1 = value(row.getCell(7));
                originalData.add(a1);
                if (a1 != null && a1.length() > 200) {
                    parsingErrors.add("FIELD:Address Line 1:TOO_LONG:Address Line 1 cannot exceed 200 characters");
                }
                dto.setAddressLine1(a1);

                // Address Line 2 validation
                String a2 = value(row.getCell(8));
                originalData.add(a2);
                if (a2 != null && a2.length() > 200) {
                    parsingErrors.add("FIELD:Address Line 2:TOO_LONG:Address Line 2 cannot exceed 200 characters");
                }
                dto.setAddressLine2(a2);

                // Pincode validation
                String pin = value(row.getCell(9));
                originalData.add(pin);
                if (pin != null && !pin.isBlank() && !pin.matches("\\d{6}")) {
                    parsingErrors.add("FIELD:Pincode:INVALID_FORMAT:Pincode must be exactly 6 digits");
                }
                dto.setPincode(pin);

                // City validation
                String city = value(row.getCell(10));
                originalData.add(city);
                if (city != null && city.length() > 50) {
                    parsingErrors.add("FIELD:City:TOO_LONG:City name cannot exceed 50 characters");
                }
                dto.setCity(city);

                // State validation
                String state = value(row.getCell(11));
                originalData.add(state);
                if (state != null && state.length() > 50) {
                    parsingErrors.add("FIELD:State:TOO_LONG:State name cannot exceed 50 characters");
                }
                dto.setState(state);

                // Country validation
                String country = value(row.getCell(12));
                originalData.add(country);
                if (country != null && country.length() > 50) {
                    parsingErrors.add("FIELD:Country:TOO_LONG:Country name cannot exceed 50 characters");
                }
                dto.setCountry(country);

                // Handle ID column if present
                String existingId = null;
                if (hasIdColumn) {
                    existingId = valueWithoutFormat(row.getCell(13));
                    originalData.add(existingId);
                    // ID validation - should be numeric if present
                    if (existingId != null && !existingId.isBlank()) {
                        try {
                            dto.setId(existingId.trim());
                        } catch (NumberFormatException e) {
                            parsingErrors.add("FIELD:ID:INVALID_FORMAT:ID must be a valid number");
                        }
                    }
                }

                parsedRows.add(
                        new ParsedRow(idx + 1, originalData, dto, null, null, null, null, null, null, parsingErrors));

            }
        }
        return parsedRows;
    }

    public static List<ParsedRow> parsePlayerExcelWithErrors(MultipartFile file) throws IOException {
        List<ParsedRow> parsedRows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Detect header format and validate
            Row header = sheet.getRow(0);
            boolean hasIdColumn = detectPlayerHeaderFormat(header);
            String[] expectedHeaders = hasIdColumn ? PLAYER_HEADERS_WITH_ID : PLAYER_HEADERS;

            // Validate header row
            for (int i = 0; i < expectedHeaders.length; i++) {
                Cell cell = header.getCell(i);
                String val = cell != null ? cell.getStringCellValue().trim() : null;
                if (!expectedHeaders[i].equals(val)) {
                    throw new IOException("Invalid header at column " + (i + 1) + ". Expected '" + expectedHeaders[i]
                            + "' but found '" + val + "'.");
                }
            }

            for (int idx = 1; idx <= sheet.getLastRowNum(); idx++) {
                Row row = sheet.getRow(idx);
                if (row == null) {
                    continue;
                }

                String name = value(row.getCell(0));
                String phone = value(row.getCell(1));
                if (!StringUtils.hasText(name) && !StringUtils.hasText(phone)) {
                    continue;
                }

                List<String> originalData = new ArrayList<>();
                List<String> parsingErrors = new ArrayList<>();
                UserProfileDto dto = new UserProfileDto();

                // Full Name -> displayName
                originalData.add(name);
                if (name == null || name.isBlank()) {
                    parsingErrors.add("FIELD:Name:REQUIRED:Name is mandatory and cannot be empty");
                } else if (name.length() > 100) {
                    parsingErrors.add("FIELD:Name:TOO_LONG:Name cannot exceed 100 characters");
                }
                dto.setDisplayName(name);

                // Phone
                originalData.add(phone);
                if (phone == null || phone.isBlank()) {
                    parsingErrors.add("FIELD:Phone Number:REQUIRED:Phone number is mandatory");
                } else {
                    phone = phone.replaceAll("[^0-9]", ""); // Remove non-digits
                    if (phone.length() != 12) {
                        parsingErrors.add(
                                "FIELD:Phone Number:INVALID_FORMAT:Phone number must be exactly 12 digits including country code (e.g., 919876543210)");
                    } else if (!phone.startsWith("91")) {
                        parsingErrors
                                .add("FIELD:Phone Number:INVALID_FORMAT:Phone number must start with country code 91");
                    }
                }
                dto.setPhoneNumber(phone);

                // Gender
                String genderStr = value(row.getCell(2));
                originalData.add(genderStr);
                if (genderStr == null || genderStr.isBlank()) {
                    parsingErrors.add("FIELD:Gender:REQUIRED:Gender is mandatory");
                } else {
                    try {
                        dto.setGender(Gender.valueOf(genderStr.trim().toUpperCase()));
                    } catch (Exception e) {
                        parsingErrors.add("FIELD:Gender:INVALID_VALUE:Gender must be valid, found: " + genderStr);
                    }
                }

                // Date of Birth
                String dobStr = value(row.getCell(3));
                originalData.add(dobStr);
                dto.setDob(dobStr);

                // String dobStr = dateValue(row.getCell(3));
                // if (dobStr != null && !dobStr.isBlank()) {
                // dto.setDob(dobStr);
                // originalData.add(dobStr); // Only add if valid
                // } else {
                // originalData.add(null); // If blank, also put null
                // parsingErrors.add("FIELD:Date of Birth:REQUIRED:Invalid date of birth format:
                // " + dobStr
                // + ". Only supported formats: dd-MMM-YYYY, dd/MMM/YYYY, dd MMM YYYY (e.g.,
                // 31-Jan-2024, 31/Jan/2024, 31 Jan 2024) "
                // + "or dd-MMMM-YYYY, dd/MMMM/YYYY, dd MMMM YYYY (e.g., 15-December-2023,
                // 15/December/2023, 15 December 2023)");
                // }

                // Email
                String email = value(row.getCell(4));
                originalData.add(email);
                if (email == null || email.isBlank()) {
                    parsingErrors.add("FIELD:Email:REQUIRED:Email address is mandatory");
                } else {
                    email = email.toLowerCase().trim();
                    if (!isValidEmailFormat(email)) {
                        parsingErrors.add(
                                "FIELD:Email:INVALID_FORMAT:Please enter a valid email address (e.g., user@example.com)");
                    } else if (email.length() > 100) {
                        parsingErrors.add("FIELD:Email:TOO_LONG:Email address cannot exceed 100 characters");
                    }
                }
                dto.setEmailId(email);

                // Address Line 1
                String a1 = value(row.getCell(5));
                originalData.add(a1);
                if (a1 != null && a1.length() > 200) {
                    parsingErrors.add("FIELD:Address Line 1:TOO_LONG:Address Line 1 cannot exceed 200 characters");
                }
                dto.setAddressLine1(a1);

                // Address Line 2
                String a2 = value(row.getCell(6));
                originalData.add(a2);
                if (a2 != null && a2.length() > 200) {
                    parsingErrors.add("FIELD:Address Line 2:TOO_LONG:Address Line 2 cannot exceed 200 characters");
                }
                dto.setAddressLine2(a2);

                // Pincode
                String pin = value(row.getCell(7));
                originalData.add(pin);
                if (pin != null && !pin.isBlank() && !pin.matches("\\d{6}")) {
                    parsingErrors.add("FIELD:Pincode:INVALID_FORMAT:Pincode must be exactly 6 digits");
                }
                dto.setPincode(pin);

                // City
                String city = value(row.getCell(8));
                originalData.add(city);
                if (city != null && city.length() > 50) {
                    parsingErrors.add("FIELD:City:TOO_LONG:City name cannot exceed 50 characters");
                }
                dto.setCity(city);

                // State
                String state = value(row.getCell(9));
                originalData.add(state);
                if (state != null && state.length() > 50) {
                    parsingErrors.add("FIELD:State:TOO_LONG:State name cannot exceed 50 characters");
                }
                dto.setState(state);

                // Country
                String country = value(row.getCell(10));
                originalData.add(country);
                dto.setCountry(country);

                // Handle ID column if present
                String existingId = null;
                if (hasIdColumn) {
                    existingId = valueWithoutFormat(row.getCell(11));
                    originalData.add(existingId);
                    // ID validation - should be numeric if present
                    if (existingId != null && !existingId.isBlank()) {
                        try {
                            dto.setId(existingId.trim());
                        } catch (NumberFormatException e) {
                            parsingErrors.add("FIELD:ID:INVALID_FORMAT:ID must be a valid number");
                        }
                    }
                }
                parsedRows.add(
                        new ParsedRow(idx + 1, originalData, dto, null, null, null, null, null, null, parsingErrors));

            }
        }
        return parsedRows;
    }

    /**
     * Detects whether the coach Excel file has ID column by checking header count
     * and last column name
     */
    private static boolean detectCoachHeaderFormat(Row header) throws IOException {
        if (header == null) {
            throw new IOException("Header row is missing");
        }

        int headerCount = header.getLastCellNum();

        // Check if we have the right number of columns
        if (headerCount == COACH_TEMPLATE_HEADERS.length) {
            return false; // Standard format without ID
        } else if (headerCount == COACH_TEMPLATE_HEADERS_WITH_ID.length) {
            // Verify the last column is indeed "ID"
            Cell lastCell = header.getCell(headerCount - 1);
            String lastHeaderValue = lastCell != null ? lastCell.getStringCellValue().trim() : null;
            if ("ID".equals(lastHeaderValue)) {
                return true; // Format with ID
            } else {
                throw new IOException("Expected 'ID' in last column but found: " + lastHeaderValue);
            }
        } else {
            throw new IOException("Invalid number of columns. Expected " + COACH_TEMPLATE_HEADERS.length + " or "
                    + COACH_TEMPLATE_HEADERS_WITH_ID.length + " but found " + headerCount);
        }
    }

    /**
     * Detects whether the player Excel file has ID column by checking header count
     * and last column name
     */
    private static boolean detectPlayerHeaderFormat(Row header) throws IOException {
        if (header == null) {
            throw new IOException("Header row is missing");
        }

        int headerCount = header.getLastCellNum();

        // Check if we have the right number of columns
        if (headerCount == PLAYER_HEADERS.length) {
            return false; // Standard format without ID
        } else if (headerCount == PLAYER_HEADERS_WITH_ID.length) {
            // Verify the last column is indeed "ID"
            Cell lastCell = header.getCell(headerCount - 1);
            String lastHeaderValue = lastCell != null ? lastCell.getStringCellValue().trim() : null;
            if ("ID".equals(lastHeaderValue)) {
                return true; // Format with ID
            } else {
                throw new IOException("Expected 'ID' in last column but found: " + lastHeaderValue);
            }
        } else {
            throw new IOException("Invalid number of columns. Expected " + PLAYER_HEADERS.length + " or "
                    + PLAYER_HEADERS_WITH_ID.length + " but found " + headerCount);
        }
    }

    // Enhanced Validation Methods
    private static boolean isValidEmailFormat(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }

    private static boolean isValidCoachRole(Role role) {
        return role == Role.COACH || role == Role.CLUSTER_HEAD || role == Role.ADMIN || role == Role.PROGRAM_MANAGER;
    }

    private static String value(Cell cell) {
        if (cell == null) {
            return null;
        }
        String rawValue;
        switch (cell.getCellType()) {
            case STRING:
                rawValue = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    try {
                        // Try with slash format
                        SimpleDateFormat sdfSlash = new SimpleDateFormat("dd/MM/yyyy");
                        rawValue = sdfSlash.format(date);
                    } catch (Exception e1) {
                        try {
                            // If slash format fails, try with dash format
                            SimpleDateFormat sdfDash = new SimpleDateFormat("dd-MM-yyyy");
                            rawValue = sdfDash.format(date);
                        } catch (Exception e2) {
                            rawValue = null; // Or set a default/fallback error value
                        }
                    }
                } else {
                    rawValue = String.valueOf((long) cell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                rawValue = String.valueOf(cell.getBooleanCellValue());
                break;
            default:
                rawValue = null;
        }

        if (rawValue == null)
            return null;

        // Normalize: trim, collapse spaces, and capitalize each word
        String cleaned = rawValue.trim().replaceAll("\\s{2,}", " ");
        return capitalizeWords(cleaned);
    }

    private static String valueWithoutFormat(Cell cell) {
        if (cell == null) {
            return null;
        }
        String rawValue;
        switch (cell.getCellType()) {
            case STRING:
                rawValue = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    rawValue = sdf.format(date);
                } else {
                    rawValue = String.valueOf((long) cell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                rawValue = String.valueOf(cell.getBooleanCellValue());
                break;
            default:
                rawValue = null;
        }

        if (rawValue == null)
            return null;

        return rawValue.trim();
    }

    private static String capitalizeWords(String input) {
        if (input == null || input.isBlank())
            return input;
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }
        return result.toString().trim();
    }

    private static String valueOfTime(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date date = cell.getDateCellValue();
                    // Check if the cell's format is a time-only format
                    String formatString = cell.getCellStyle().getDataFormatString().toLowerCase();
                    boolean isTimeFormat = formatString.contains("h") || formatString.contains("m");

                    if (isTimeFormat) {
                        // Format as 24-hour time (HH:mm)
                        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                        return timeFormat.format(date);
                    } else {
                        // Format as date (dd-MM-yyyy)
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                        return dateFormat.format(date);
                    }
                } else {
                    // Handle pure numeric values (non-date)
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    public static List<ParsedRow> parseProgramExcelWithErrors(MultipartFile file) throws IOException {
        List<ParsedRow> parsedRows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Validate headers
            Row header = sheet.getRow(0);
            if (header == null || header.getPhysicalNumberOfCells() < PROGRAM_HEADERS.length) {
                throw new IOException("Invalid or missing header row.");
            }
            for (int i = 0; i < PROGRAM_HEADERS.length; i++) {
                String cellValue = value(header.getCell(i));
                if (cellValue == null || !PROGRAM_HEADERS[i].equalsIgnoreCase(cellValue.trim())) {
                    throw new IOException("Invalid header at column " + (i + 1) + ". Expected '" + PROGRAM_HEADERS[i]
                            + "', but found '" + cellValue + "'");
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String title = value(row.getCell(0));
                if (!StringUtils.hasText(title)) {
                    continue;
                }

                List<String> originalData = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                CreateCourseDto dto = new CreateCourseDto();
                ScheduleDto schedule = new ScheduleDto();

                // Title
                originalData.add(title);
                if (isBlank(title)) {
                    errors.add("FIELD:Title:REQUIRED:Title is mandatory and cannot be empty");
                }
                dto.setTitle(title);

                // Description
                String description = value(row.getCell(1));
                originalData.add(description);
                if (isBlank(description)) {
                    errors.add("FIELD:Description:REQUIRED:Description is mandatory and cannot be empty");
                }
                dto.setDescription(description);

                // Sport
                String sport = value(row.getCell(2));
                originalData.add(sport);
                if (isBlank(sport)) {
                    errors.add("FIELD:Sport:REQUIRED:Sport is mandatory and cannot be empty");
                } else {
                    try {
                        dto.setSport(Sports.valueOf(sport.trim().toUpperCase()));
                    } catch (Exception e) {
                        errors.add("FIELD:Sport:INVALID_VALUE:Invalid Sport value: " + sport);
                    }
                }

                // Skill Level → level
                String skill = value(row.getCell(3));
                originalData.add(skill);
                if (isBlank(skill)) {
                    errors.add("FIELD:Skill Level:REQUIRED:Skill Level is mandatory and cannot be empty");
                } else {
                    try {
                        dto.setLevel(SkillLevel.valueOf(skill.trim().toUpperCase()));
                    } catch (Exception e) {
                        errors.add("FIELD:Skill Level:INVALID_VALUE:Invalid Skill Level: " + skill);
                    }
                }

                // Age Category (required)
                String ageCategory = value(row.getCell(4));
                originalData.add(ageCategory);
                if (isBlank(ageCategory)) {
                    errors.add("FIELD:Age Category:REQUIRED:Age category is mandatory and cannot be empty");
                } else {
                    try {
                        dto.setAgeGroup(ageCategory.toUpperCase());
                    } catch (Exception e) {
                        errors.add("FIELD:Age Category:INVALID_VALUE:Invalid Age Category: " + ageCategory);
                    }
                }

                // Total Max Trainees
                String maxTrainees = value(row.getCell(5));
                originalData.add(maxTrainees);
                if (isBlank(maxTrainees)) {
                    errors.add("FIELD:Total Max Trainees:REQUIRED:Total Max Trainees is mandatory and cannot be empty");
                } else {
                    try {
                        long maxTraineesValue = Long.parseLong(maxTrainees);
                        if (maxTraineesValue <= 0) {
                            errors.add(
                                    "FIELD:Total Max Trainees:INVALID_VALUE:Total Max Trainees must be a positive number: "
                                            + maxTrainees);
                        } else {
                            dto.setTotalMaxTrainees(maxTraineesValue);
                        }
                    } catch (NumberFormatException e) {
                        errors.add("FIELD:Total Max Trainees:INVALID_VALUE:Total Max Trainees must be a valid number: "
                                + maxTrainees);
                    }
                }

                // Visibility
                String visibility = value(row.getCell(6));
                originalData.add(visibility);
                if (isBlank(visibility)) {
                    errors.add("FIELD:Visibility:REQUIRED:Visibility is mandatory and cannot be empty");
                } else {
                    try {
                        dto.setVisibility(Visibility.valueOf(visibility.trim().toUpperCase()));
                    } catch (Exception e) {
                        errors.add("FIELD:Visibility:INVALID_VALUE:Invalid Visibility: " + visibility);
                    }
                }

                // Registration Fee (optional, defaults to 0)
                String fee = value(row.getCell(7));
                originalData.add(fee);
                if (fee == null || fee.trim().isEmpty()) {
                    dto.setRegistrationFee(0L);
                } else {
                    try {
                        long feeValue = Long.parseLong(fee);
                        if (feeValue < 0) {
                            errors.add(
                                    "FIELD:Registration Fee:INVALID_VALUE:Registration Fee cannot be negative: " + fee);
                        } else {
                            dto.setRegistrationFee(feeValue);
                        }
                    } catch (NumberFormatException e) {
                        errors.add("FIELD:Registration Fee:INVALID_VALUE:Invalid Registration Fee format: " + fee);
                    }
                }

                // Currency
                String currency = value(row.getCell(8));
                originalData.add(currency);
                if (isBlank(currency)) {
                    errors.add("FIELD:Currency:REQUIRED:Currency is mandatory and cannot be empty");
                } else {
                    try {
                        schedule.setCurrency(Currency.valueOf(currency.trim().toUpperCase()));
                    } catch (Exception e) {
                        errors.add("FIELD:Currency:INVALID_VALUE:Invalid Currency type: " + currency);
                    }
                }

                // Schedule Type
                String scheduleType = value(row.getCell(9));
                originalData.add(scheduleType);
                if (isBlank(scheduleType)) {
                    errors.add("FIELD:Schedule Type:REQUIRED:Schedule Type is mandatory and cannot be empty");
                } else {
                    try {
                        schedule.setType(ScheduleType.valueOf(scheduleType.trim().toUpperCase()));
                    } catch (Exception e) {
                        errors.add("FIELD:Schedule Type:INVALID_VALUE:Invalid Schedule Type: " + scheduleType);
                    }
                }

                // Days of Week
                String days = value(row.getCell(10));
                originalData.add(days);
                if (!isBlank(days)) {
                    List<DayOfWeek> dayList = new ArrayList<>();
                    for (String d : days.split(",")) {
                        try {
                            dayList.add(DayOfWeek.valueOf(d.trim().toUpperCase()));
                        } catch (Exception e) {
                            errors.add("FIELD:Days of Week:INVALID_VALUE:Invalid day of week: " + d.trim());
                        }
                    }
                    schedule.setWeekdays(dayList);
                }

                schedule.setAmount(0l);

                List<String> rules = new ArrayList<>();
                rules.add("Bring your own gears.");
                schedule.setRulesAndRegulations(rules);

                // Start Date
                String startDateStr = dateValue(row.getCell(11));
                if (startDateStr != null && !startDateStr.isBlank()) {
                    try {
                        LocalDate joiningDate = parseNewDate(startDateStr.trim());
                        schedule.setStartDate(joiningDate.toString()); // Store standardized date, or adjust type
                        originalData.add(joiningDate.toString());
                    } catch (DateTimeParseException e) {
                        schedule.setStartDate(null);
                        originalData.add(null);
                        errors.add(
                                "FIELD:Start Date:INVALID_FORMAT:Invalid start date format. Supported formats: dd-MMM-yyyy, dd/MMM/yyyy, dd MMM yyyy "
                                        + "(e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024) or "
                                        + "dd-MMMM-yyyy, dd/MMMM/yyyy, dd MMMM yyyy "
                                        + "(e.g., 15-December-2023, 15/December/2023, 15 December 2023)");
                    }
                } else {
                    schedule.setStartDate(null);
                    originalData.add(null);
                    errors.add("FIELD:Start Date:REQUIRED:Start Date is mandatory and cannot be empty");
                }

                // End Date
                String endDateStr = dateValue(row.getCell(12));
                if (endDateStr != null && !endDateStr.isBlank()) {
                    try {
                        LocalDate endDate = parseNewDate(endDateStr.trim());
                        schedule.setEndDate(endDate.toString()); // Or pass LocalDate if your model supports it
                        originalData.add(endDate.toString());
                    } catch (DateTimeParseException e) {
                        schedule.setEndDate(null);
                        originalData.add(null);
                        errors.add(
                                "FIELD:End Date:INVALID_FORMAT:Invalid end date format. Supported formats: dd-MMM-yyyy, dd/MMM/yyyy, dd MMM yyyy "
                                        + "(e.g., 31-Jan-2024, 31/Jan/2024, 31 Jan 2024) or "
                                        + "dd-MMMM-yyyy, dd/MMMM/yyyy, dd MMMM yyyy "
                                        + "(e.g., 15-December-2023, 15/December/2023, 15 December 2023)");
                    }
                } else {
                    schedule.setEndDate(null);
                    originalData.add(null);
                    errors.add("FIELD:End Date:REQUIRED:End Date is mandatory and cannot be empty");
                }

                // Session Start Time
                String startTime = valueOfTime(row.getCell(13));
                originalData.add(startTime);
                if (isBlank(startTime)) {
                    errors.add("FIELD:Session Start Time:REQUIRED:Session Start Time is mandatory and cannot be empty");
                } else {
                    schedule.setStartTime(startTime);
                }

                // Session End Time
                String endTime = valueOfTime(row.getCell(14));
                originalData.add(endTime);
                if (isBlank(endTime)) {
                    errors.add("FIELD:Session End Time:REQUIRED:Session End Time is mandatory and cannot be empty");
                } else {
                    schedule.setEndTime(endTime);
                }

                dto.setSchedule(schedule);

                // Payment Schedule Types
                String typesStr = value(row.getCell(15));
                originalData.add(typesStr);

                // Payment Amounts
                String amountsStr = value(row.getCell(16));
                originalData.add(amountsStr);

                Map<PaymentSchedule, Long> paymentMap = new LinkedHashMap<>();
                if (!isBlank(typesStr) || !isBlank(amountsStr)) {
                    if (isBlank(typesStr) || isBlank(amountsStr)) {
                        errors.add(
                                "FIELD:Payment Options:REQUIRED:Both Payment Types and Amounts must be provided when either is specified");
                    } else {
                        String[] types = typesStr.split(",");
                        String[] amounts = amountsStr.split(",");
                        if (types.length != amounts.length) {
                            errors.add("FIELD:Payment Options:INVALID_VALUE:Payment types and amounts count mismatch");
                        } else {
                            for (int t = 0; t < types.length; t++) {
                                try {
                                    PaymentSchedule type = PaymentSchedule.valueOf(types[t].trim().toUpperCase());
                                    Long amt = Long.parseLong(amounts[t].trim());
                                    if (amt <= 0) {
                                        errors.add(
                                                "FIELD:Payment Options:INVALID_VALUE:Payment amount must be positive at index "
                                                        + (t + 1));
                                    }
                                    paymentMap.put(type, amt);
                                } catch (IllegalArgumentException e) {
                                    if (e.getMessage().contains("PaymentSchedule")) {
                                        errors.add("FIELD:Payment Options:INVALID_VALUE:Invalid payment type '"
                                                + types[t].trim() + "' at index " + (t + 1));
                                    } else {
                                        errors.add("FIELD:Payment Options:INVALID_VALUE:Invalid payment amount '"
                                                + amounts[t].trim() + "' at index " + (t + 1));
                                    }
                                } catch (Exception e) {
                                    errors.add("FIELD:Payment Options:INVALID_VALUE:Invalid payment data at index "
                                            + (t + 1));
                                }
                            }
                        }
                    }
                }
                dto.setPaymentOptions(paymentMap);

                parsedRows.add(new ParsedRow(i + 1, originalData, null, dto, null, null, null, null, null, errors));
            }
        }

        return parsedRows;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static List<ParsedRow> parseProgramCoachExcelWithErrors(MultipartFile file) {

        List<ParsedRow> parsedRows = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is null or empty");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet mappingSheet = workbook.getSheet("Program-Coach Mapping");
            Sheet validationSheet = workbook.getSheet("ValidationData");

            if (mappingSheet == null) {
                throw new IOException("Sheet 'Program-Coach Mapping' not found in Excel.");
            }
            if (validationSheet == null) {
                throw new IOException("Sheet 'ValidationData' not found in Excel.");
            }

            // Build maps: Program Name -> ID, Coach Name -> ID
            Map<String, String> programIdMap = buildIdMap(validationSheet, 0); // Column A & B
            Map<String, String> coachIdMap = buildIdMap(validationSheet, 3); // Column D & E

            for (int i = 1; i <= mappingSheet.getLastRowNum(); i++) {
                Row row = mappingSheet.getRow(i);
                if (row == null) {
                    continue;
                }

                ParsedRow parsedRow = new ParsedRow(i + 1, null, null, null, null, null, null, null, null, null);
                parsedRow.setParsingErrors(new ArrayList<>());

                String programName = getCellValue(row.getCell(0));
                String coachName = getCellValue(row.getCell(1));

                // Program DTO
                ParsedRow.ProgramDto program = new ParsedRow.ProgramDto();
                program.setProgramName(programName);
                program.setProgramId(programIdMap.get(programName));
                parsedRow.setProgram(program);

                // Coach DTO
                ParsedRow.CoachDto coach = new ParsedRow.CoachDto();
                coach.setCoachName(coachName);
                coach.setCoachId(coachIdMap.get(coachName));
                parsedRow.setCoach(coach);

                // Store original values
                parsedRow.setOriginalData(
                        Arrays.asList(programName, coachName, program.getProgramId(), coach.getCoachId()));

                // Validation
                if (!StringUtils.hasText(program.getProgramId())) {
                    parsedRow.getParsingErrors().add("Missing Program ID");
                }
                if (!StringUtils.hasText(coach.getCoachId())) {
                    parsedRow.getParsingErrors().add("Missing Coach ID");
                }

                parsedRows.add(parsedRow);
            }

        } catch (IllegalArgumentException | IOException e) {
            throw new RuntimeException("Excel parsing error: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace(); // Add this line for better diagnosis during local/dev testing
            throw new RuntimeException("Unexpected error while parsing Excel file: "
                    + (e.getMessage() != null ? e.getMessage() : e.toString()), e);
        }

        return parsedRows;
    }

    private static Map<String, String> buildIdMap(Sheet validationSheet, int startCol) {
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i <= validationSheet.getLastRowNum(); i++) {
            Row row = validationSheet.getRow(i);
            if (row == null) {
                continue;
            }

            String name = getCellValue(row.getCell(startCol));
            String id = getCellValue(row.getCell(startCol + 1));
            if (StringUtils.hasText(name) && StringUtils.hasText(id)) {
                map.put(name.trim(), id.trim());
            }
        }
        return map;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    // Updated helper to evaluate formulas
    private static String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        CellValue cellValue = evaluator.evaluate(cell);
        if (cellValue == null) {
            return null;
        }

        switch (cellValue.getCellType()) {
            case STRING:
                return cellValue.getStringValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new DataFormatter().formatCellValue(cell, evaluator);
                }
                double num = cellValue.getNumberValue();
                if (num == Math.floor(num)) {
                    return String.valueOf((long) num);
                } else {
                    return String.valueOf(num);
                }
            case BOOLEAN:
                return String.valueOf(cellValue.getBooleanValue());
            case BLANK:
                return null;
            case ERROR:
                return null;
            default:
                return null;
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static List<ParsedRow> parseProgramPlayersExcelWithErrors(MultipartFile file) {
        List<ParsedRow> parsedRows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet mappingSheet = workbook.getSheet("Program-Player Mapping");
            Sheet validationSheet = workbook.getSheet("ValidationData");

            if (mappingSheet == null) {
                throw new IOException("Sheet 'Program-Player Mapping' not found in Excel.");
            }

            if (validationSheet == null) {
                throw new IOException("Sheet 'ValidationData' not found in Excel.");
            }

            // Build maps: Program Name -> ID, Player Name -> ID
            Map<String, String> programIdMap = buildIdMap(validationSheet, 0); // Column A & B
            Map<String, String> playerIdMap = buildIdMap(validationSheet, 3); // Column D & E

            // Process the data rows
            for (int i = 1; i <= mappingSheet.getLastRowNum(); i++) {
                Row row = mappingSheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // Check if the row is actually empty (has no cell values)
                boolean isEmpty = true;
                for (int j = 0; j <= 5; j++) { // Check visible columns
                    String cellValue = value(row.getCell(j));
                    if (cellValue != null && !cellValue.isEmpty()) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    continue; // Skip truly empty rows
                }

                List<String> originalData = new ArrayList<>();
                List<String> errors = new ArrayList<>();

                // Read visible columns
                String programName = value(row.getCell(0));
                originalData.add(programName);

                String playerName = value(row.getCell(1));
                originalData.add(playerName);

                String paymentScheduleStr = value(row.getCell(2));
                originalData.add(paymentScheduleStr);

                String amountStr = getCellValueAsString(row.getCell(3));
                originalData.add(amountStr);

                String joiningDateStr = value(row.getCell(4));
                originalData.add(joiningDateStr);

                String dueDateStr = value(row.getCell(5));
                originalData.add(dueDateStr);

                // Read hidden columns for IDs (formulas or direct values)
                String programId = value(row.getCell(7)); // Hidden Program ID
                String playerId = value(row.getCell(8)); // Hidden Player ID

                // If IDs are not found in hidden cells, try lookup from the maps
                if ((programId == null || programId.isEmpty()) && programName != null) {
                    programId = programIdMap.get(programName.trim());
                }

                if ((playerId == null || playerId.isEmpty()) && playerName != null) {
                    playerId = playerIdMap.get(playerName.trim());
                }

                // Validate required fields
                if (programName == null || programName.isEmpty()) {
                    errors.add("Program is missing");
                } else if (programId == null || programId.isEmpty()) {
                    errors.add("Program ID not found for: " + programName);
                }

                if (playerName == null || playerName.isEmpty()) {
                    errors.add("Player is missing");
                } else if (playerId == null || playerId.isEmpty()) {
                    errors.add("Player ID not found for: " + playerName);
                }

                if (paymentScheduleStr == null || paymentScheduleStr.isEmpty()) {
                    errors.add("Payment Schedule is missing");
                }

                if (amountStr == null || amountStr.isEmpty()) {
                    errors.add("Amount is missing");
                }

                TraineeCourseEnrollmentDto enrollmentDto = new TraineeCourseEnrollmentDto();

                try {
                    enrollmentDto.setPaymentSchedule(PaymentSchedule.valueOf(paymentScheduleStr.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    errors.add("Invalid Payment Schedule: " + paymentScheduleStr);
                }

                Long amount = null;
                try {
                    if (amountStr != null && !amountStr.isEmpty()) {
                        amount = Long.parseLong(amountStr);
                        if (amount <= 0) {
                            errors.add("Amount must be positive");
                        }
                    }
                } catch (NumberFormatException e) {
                    errors.add("Invalid Amount: " + amountStr);
                }

                // Parse dates
                LocalDate joiningDate = null;
                if (joiningDateStr != null && !joiningDateStr.isEmpty()) {
                    try {
                        joiningDate = LocalDate.parse(sanitize(joiningDateStr),
                                DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    } catch (DateTimeParseException e) {
                        errors.add("Invalid Joining Date format. Expected dd-MM-yyyy: " + joiningDateStr);
                    }
                } else {
                    errors.add("Joining Date is required");
                }

                LocalDate dueDate = null;
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    try {
                        dueDate = LocalDate.parse(sanitize(dueDateStr), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    } catch (DateTimeParseException e) {
                        errors.add("Invalid Due Date format. Expected dd-MM-yyyy: " + dueDateStr);
                    }
                }

                // Create enrollment DTO
                enrollmentDto.setTraineeUserId(playerId);
                enrollmentDto.setAmount(amount);
                enrollmentDto.setJoiningDate(joiningDate);
                enrollmentDto.setDueDate(dueDate);

                // Create ParsedRow object
                ParsedRow parsedRow = new ParsedRow();
                parsedRow.setRowNumber(i + 1); // 1-based row number for user display
                parsedRow.setOriginalData(originalData);
                parsedRow.setEnrollmentDto(enrollmentDto);
                parsedRow.setParsingErrors(errors);

                // Set program and player information
                ParsedRow.ProgramDto program = new ParsedRow.ProgramDto(programName, programId);
                parsedRow.setProgram(program);

                ParsedRow.PlayerDto player = new ParsedRow.PlayerDto(playerName, playerId);
                parsedRow.setPlayer(player);

                parsedRows.add(parsedRow);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while parsing Excel file: " + e.getMessage(), e);
        }

        return parsedRows;
    }

    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Replace non-breaking spaces, slashes, and trim
        return input.replace('\u00A0', ' ').replace("/", "-") // Handle slashes in dates
                .trim();
    }

    // Helper method to check if a row is empty
    private static boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            if (row.getCell(i) != null && !row.getCell(i).toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMATTER.format(cell.getLocalDateTimeCellValue().toLocalDate());
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        return String.valueOf((long) cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return null; // If formula evaluation fails, return null
                    }
                }
            case ERROR:
                // Handle ERROR cell type by returning null or a placeholder
                return null;
            case BLANK:
            case _NONE:
            default:
                return null;
        }
    }

    private static Long getCellValueAsLong(Cell cell) {
        String value = getCellValueAsString(cell);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
