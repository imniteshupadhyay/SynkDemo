package com.playmotech.api.core.response;

import java.util.HashSet;
import java.util.Set;

import lombok.extern.log4j.Log4j2;

@Log4j2
public enum ApiResponse {
	INTERNAL_SERVER("Something unexpected happened. Please try again", "500"),

	FILE_PROCESSING_ERROR("Error processing file.", "U-003"),

	RESOURCE_NOT_FOUND("Resource not found.", "U-103"),

	USERS_UPLOADED("Users uploaded successfully", "U-007"),

	LOGIN_SUCCESSFUL("Login successful", "U-200"), 
	LOGIN_FAILED("Login failed due to an internal error", "U-201"),

	USER_FOUND("User found successfully", "U-001"), 
	USER_FETCH_FAILED("Failed to fetch user details", "U-002"),
	USER_NOT_FOUND_IN_ACADEMY("User not found in the academy", "U-104"),

	PASSWORD_UPDATED_SUCCESSFULLY("Password updated successfully", "U-101"),
	INVALID_CURRENT_PASSWORD("Current password is incorrect", "U-102"),
	PASSWORD_UPDATE_FAILED("Failed to update password", "U-105"),

	PLAYERS_ENROLLED_SUCCESSFULLY("Players enrolled successfully", "U-107"),

	COACHES_MAPPED("Coaches mapped successfully", "U-108"), 
	PARTIAL_UPLOAD("Excel parsed, Please verify...", "U-008"),
	INVALID_EXCEL_DATA("No valid user data. See error report.", "U-009"),

	INVALID_PATH("Invalid or empty path provided.", "BT-001"),
	PATH_NOT_FOUND("The specified path does not exist.", "BT-002"),
	NOT_A_DIRECTORY("The specified path is not a directory.", "BT-003"),
	WRITE_PERMISSION_DENIED("Write permission denied for path.", "BT-004"),
	TEMPLATE_CREATED("Template created successfully.", "BT-005"),
	TEMPLATE_OVERWRITTEN("Template overwritten successfully.", "BT-006"),
	FILE_WRITE_ERROR("Failed to write the file.", "BT-007"),

	DUES_UPDATED_SUCCESSFULLY("Dues updated successfully", "DUES-001"),
	NO_MORE_INSTALLMENTS("No more future installments", "DUES-002"),
	TOO_EARLY_FOR_NEXT_DUE("Too early for next due date", "DUES-003"),
	INSTALLMENT_INFO_NOT_FOUND("Installment info not found for due date", "DUES-004"),
	ENROLLMENT_NOT_FOUND("Enrollment not found", "DUES-005"),
	COURSE_DETAILS_NOT_FOUND("Enrollment or course details missing", "DUES-006"),
	DUE_DATES_GENERATION_FAILED("No due dates generated from schedule", "DUES-007"),
	DUES_UPDATE_FAILED("Unexpected error while updating dues", "DUES-008"),

	COURSES_CREATED("Programs uploaded successfully", "U-109"),

	UNEXPECTED_ERROR("Unexpected error occurred.", "BT-008"),

	NO_DATA_FOUND("No valid data. See error report.", "BT-009"),

	USER_CREATED("User created successfully", "U-110"),
	USER_UPDATED("User updated successfully", "U-111"),
	USER_NOT_FOUND("User not found", "U-112"),
	DUPLICATE_USER("A user with the same username/phone already exists", "U-004"),
	ERROR_CREATING_USER("Error occurred while creating user", "U-113"),
	ERROR_UPDATING_USER("Error occurred while updating user", "U-006"),

	// Generic Response Message
	LIST_FETCHED_SUCCESSFULLY("List fetched successfully", "G-001"),
	ERROR_FETCHING_LIST("Error while fetching list", "G-002"),
	DATA_ADDED_SUCCESSFULLY("Data added successfully", "G-003"),
	DATA_UPDATED_SUCCESSFULLY("Data updated successfully", "G-004"),
	DATA_DELETED_SUCCESSFULLY("Data deleted successfully", "G-005"), 
	ERROR_ADDING_DATA("Error while adding", "G-006"),
	ERROR_UPDATING_DATA("Error while updating", "G-007"),
	FILE_UPLOADED_SUCCESSFULLY("File uploaded successfully", "G-008"),

	// Role Responses
	ROLE_ADD_SUCCESS("Role added successfully", "R-001"), 
	ROLE_UPDATE_SUCCESS("Role updated successfully", "R-002"),
	DUPLICATE_ROLE_NAME("Same role already exists", "R-003"),
	MODULES_ACTION_ALTERED("Modules or Modules Actions has been edited. Please check and try again", "R-004"),

	// Attendance Responses
	ATTENDANCE_NOT_FOUND("No attendance records found", "ATT-001"),
	ATTENDANCE_LIST_FETCHED("Attendance list fetched successfully", "ATT-002"),
	ERROR_FETCHING_ATTENDANCE_LIST("Error occurred while fetching attendance records", "ATT-003"),

	// Sports Responses
	SPORTS_LIST_FETCHED("Sports list fetched successfully", "SP-001"), 
	SPORTS_NOT_FOUND("No sports found", "SP-002"),

	PAYMENT_KPI_FETCHED("Payment KPI fetched successfully", "PKI-001"),
	ERROR_FETCHING_PAYMENT_KPIS("Error fetching payment KPIs", "PKI-002"),

	// Payment Trend
	PAYMENT_TRENDS_FETCHED("Payment trends fetched successfully", "P-001"),
	PAYMENT_TRENDS_NOT_FOUND("No payment trends found", "P-002"),
	ERROR_FETCHING_PAYMENT_TRENDS("Error occurred while fetching payment trends", "P-003"),

	// Course Enrollment Details
	COURSE_ENROLLMENT_FETCHED("Course enrollment details fetched successfully", "CE-001"),
	COURSE_ENROLLMENT_NOT_FOUND("No course enrollment details found", "CE-002"),
	ERROR_FETCHING_COURSE_ENROLLMENT("Error occurred while fetching course enrollment details", "CE-003"),
	COURSE_PLAYER_ENROLLMENT_NOT_FOUND("No course enrollment player added details found", "CE-004"),

	// Attendance Trend
	ATTENDANCE_TRENDS_FETCHED("Attendance trends fetched successfully", "ATR-001"),
	ATTENDANCE_TRENDS_NOT_FOUND("No attendance trends found", "ATR-002"),
	ERROR_FETCHING_ATTENDANCE_TRENDS("Error occurred while fetching attendance trends", "ATR-003"),

	// Player Added Count Details
	PLAYER_ADDED_COUNT_FETCHED("Player added count fetched successfully", "PE-001"),
	PLAYER_ADDED_COUNT_NOT_FOUND("No player added count enrollment found", "PE-002"),
	ERROR_FETCHING_PLAYER_ADDED_COUNT("Error occurred while fetching player added count", "PE-003"),

	// Attendance Percentage
	ATTEDNANCE_PERCENTAGE_FETCHED("Attendance percentage fetched successfully", "APE-001"),
	ATTEDNANCE_PERCENTAGE_NOT_FOUND("No attendance percentage found", "APE-002"),
	ERROR_FETCHING_ATTEDNANCE_PERCENTAGE("Error occurred while fetching attendance percentage", "APE-003"),

	// Payments KPI
	PAYMENTS_KPI_FETCHED("Payment KPIs fetched successfully", "PKPI-001"),
	PAYMENTS_KPI_NOT_FOUND("No payment KPI data found", "PKPI-002"),
	ERROR_FETCHING_PAYMENTS_KPI("Error occurred while fetching payment KPI data", "PKPI-003"),

	// Academy Responses (Corrected Codes)
	ACADEMY_FETCHED("Academy fetched successfully.", "AC-001"),
	ACADEMY_LIST_FETCHED("Academy list fetched successfully.", "AC-002"),
	ACADEMY_ADDED("Academy added successfully.", "AC-003"), 
	ACADEMY_UPDATED("Academy updated successfully.", "AC-004"),
	ACADEMY_DELETED("Academy deleted successfully.", "AC-005"),

	// Academy Error Responses
	INVALID_ACADEMY_ID("Invalid academy ID.", "AC-006"), 
	ACADEMY_NOT_FOUND("Academy not found.", "AC-007"),
	ERROR_FETCHING_ACADEMY("Error fetching academy details.", "AC-008"),
	ERROR_SAVING_ACADEMY("Error saving academy details.", "AC-009"),
	ERROR_UPDATING_ACADEMY("Error updating academy details.", "AC-010"),
	ERROR_DELETING_ACADEMY("Error deleting academy.", "AC-011"),

	// Branch Responses
	BRANCH_FETCHED("Branch fetched successfully.", "B-001"),
	BRANCH_LIST_FETCHED("Branch list fetched successfully.", "B-002"),
	BRANCH_ADDED("Branch added successfully.", "B-003"), 
	BRANCH_UPDATED("Branch updated successfully.", "B-004"),
	BRANCH_DELETED("Branch deleted successfully.", "B-005"),

	// Branch Error Responses
	INVALID_BRANCH_ID("Invalid branch ID.", "B-006"), 
	BRANCH_NOT_FOUND("Branch not found.", "B-007"),
	ERROR_FETCHING_BRANCH("Error fetching branch details.", "B-008"),
	ERROR_SAVING_BRANCH("Error saving branch details.", "B-009"),
	ERROR_UPDATING_BRANCH("Error updating branch details.", "B-010"),
	ERROR_DELETING_BRANCH("Error deleting branch.", "B-011"),

	// Program Responses
	PROGRAM_FETCHED("Program fetched successfully.", "PRG-001"),
	PROGRAM_LIST_FETCHED("Program list fetched successfully.", "PRG-002"),
	PROGRAM_NOT_FOUND("No programs found for this academy.", "PRG-003"),

	PERFORMANCE_LIST_FETCHED("Performance list fetched successfully", "TP-001"),
	PERFORMANCE_NOT_FOUND("No performance records found", "TP-002"),
	ERROR_FETCHING_PERFORMANCE_LIST("Error occurred while fetching performance records", "TP-003"),

	START_OTP_VERIFICATION("Initiating OTP verification process", "V-001"),
	INVALID_OTP_REQUEST("Invalid OTP request: Email or OTP is missing", "V-002"),
	OTP_NOT_FOUND("OTP not found", "V-003"),
	OTP_VERIFICATION_NOT_INITIATED("OTP verification has not been initiated", "V-004"),
	OTP_VERIFIED_SUCCESSFULLY("OTP verification successful", "V-007"),
	EXCEPTION_IN_OTP_VERIFICATION("An exception occurred during the OTP verification process", "V-008"),

	USER_ID_MISMATCH("The provided user does not match the account", "U-114"),

	TEMP_PASSWORD_SENT("Temporary password has been sent successfully", "U-115"),
	UNVERIFIED_IDENTIFIER("The identifier has not been verified", "U-202"),

	VALIDATING_ID("Validating the user ID", "U-116"), 
	INVALID_USER_ID("The provided user ID is invalid", "U-117"), 
	USER_FETCHED("User details retrieved successfully", "U-118"),
	RESPONSE_ERROR("An error occurred while fetching response from the user service", "RE-001"),
	VALIDATING_EMAIL("Validating the user email address", "U-119"), 
	INVALID_EMAIL("The email format is invalid", "U-120"),
	MULTIPLE_USERS_FOUND("Multiple users found associated with the provided email", "U-121"),
	OTP_EMAIL_FAILED("Failed to send the OTP email", "A-009"),
	START_AUTHENTICATION("Initiating the authentication process", "AOTH-001"),
	OTP_SENT("OTP successfully sent to your registered email address", "AOTH-002"),
	OTP_ALREADY_USED("This OTP has already been used. Please request a new one.", "AOTH-003"),
	OTP_EXPIRED("This OTP has expired. Please request a new one.", "AOTH-004"),
	RESET_OTP_SENT_EXISTING_RECORD("OTP has been reset and sent to the existing record", "AOTH-005"),
	RESET_OTP_SENT_NEW_RECORD("OTP has been created and sent to the new record", "AOTH-006"),
	EXCEPTION_IN_AUTHENTICATION("An exception occurred during the authentication process", "AOTH-007"),

	PASSWORD_CHANGED_SUCCESSFULLY("Password changed successfully", "AUTH-001"),
	LOGIN_SUCCESS("Successfully logged in", "AUTH-002"),
	RESET_EMAIL_SENT("Instructions for password reset have been sent to your email", "AUTH-003"),
	RESET_OTP_SENT("Password reset OTP has been sent to your email", "AUTH-004"),
	USER_DELETED_SUCCESSFULY("User successfully deleted", "AUTH-005"),
	USER_ACTIVATED_SUCCESSFULLY("User successfully activated", "AUTH-006"), 
	VALID_OTP("OTP is valid", "AUTH-007"),
	VALID_TOKEN("Token is valid", "AUTH-008"),
	PASSWORD_CHANGE_SUCCESS("Password updated successfully. Log in with your new password.", "AUTH-009"),
	REGISTER_SUCCESS("User Registered successfully", "AUTH-010"), 
	TOKEN_REFRESHED("Token Refreshed Successfully", "AUTH-011"),
	USER_INACTIVATED_SUCCESSFULLY("User inactivated successfully", "AUTH-012"),
	COMPANY_DETAILS_REQUIRED("Company details are required for company users", "U-122"),

	// Client Error Codes (4xx)
	INVALID_CREDENTIALS("Invalid credentials. Please check it again.", "B-012"),
	NO_RECORD_FOUND("No record found", "B-013"),
	NOT_FOUND_PROFILE("User not found", "B-014"),
	USERNAME_NOTFOUND("Username does not exist. Please register.", "B-015"),
	PASSWORD_ERROR("Password is mandatory", "B-016"),
	USERNAME_ERROR("Username is mandatory", "B-017"),
	LOGIN_BODY_ERROR("Login body is mandatory", "B-018"),
	PASSWORD_MISMATCH("New password and confirm password do not match", "B-019"),
	INVALID_REQUEST("Invalid or missing request parameters", "B-020"),
	INVALID_TOKEN("Invalid token", "B-021"),
	INVALID_OTP("Incorrect OTP, please try again", "B-022"),
	INVALID_LENGTH_OR_REGEX("Invalid input format. Please check the provided data.", "B-023"),
	INVALID_LISTING_FILTERS("Invalid listing filters. Please provide valid parameters", "B-024"),
	ID_MISMATCH("User mismatch. Unable to update password.", "B-025"), // Changed from B-015 (duplicate)

	// Conflict Codes (4xx)
	USERNAME_CONFLICT("Username already exists", "C-001"), 
	EMAIL_CONFLICT("Email ID already exists", "C-002"),
	DUPLICATE_EMAIL("Email is already in use. Please choose a different one.", "C-003"),
	MOBILE_UPDATE_ERROR("Mobile number cannot be updated", "C-004"),
	USER_ALREADY_ACTIVE("User is already active", "C-005"),
	USER_NOT_ACTIVE_MESSAGE("Inactive user. Cannot delete.", "C-006"),
	PROCESS_NOT_INITIATED("Please reinitiate the forgot password process", "C-008"),
	LINK_EXPIRED_OR_USED("The link has expired or already been used. Please request a new one.", "C-009"),

	// Server Error Codes (5xx)
	NULL_POINTER_ERROR("Null Pointer Exception Occurred", "D-001"),
	ERROR_FETCHING_RECORD("Error while fetching record", "D-002"),

	// App Version Success Codes
	VERSION_FETCHED("Latest Version fetched successfully", "E-001"),
	VERSION_UPDATED("App Version is updated successfully", "E-002"),
	FCM_UPDATED("FCM Details updated successfully", "E-003"),

	// Generic Response Messages
	EXCEPTION_IN_ADD_VIDEO_ANALYZER("Exception occurred while adding Video Analyzer", "G-009"), // Changed from G-002 (duplicate)
	EXCEPTION_IN_UPDATE_VIDEO_ANALYZER("Exception occurred while updating Video Analyzer", "G-010"), // Changed from G-003 (duplicate)
	EXCEPTION_IN_DELETE_VIDEO_ANALYZER("Exception occurred while deleting Video Analyzer", "G-011"), // Changed from G-004 (duplicate)
	EXCEPTION_IN_VIDEO_ANALYSIS("Exception occurred during video analysis", "G-012"), // Changed from G-005 (duplicate)

	// Video Analyzer Specific Responses
	VIDEO_ANALYZER_NOT_FOUND("Video Analyzer not found", "VA-001"),
	VIDEO_ANALYZER_FETCHED("Video Analyzer fetched successfully", "VA-002"),
	VIDEO_ANALYZER_ADDED("Video Analyzer added successfully", "VA-003"),
	VIDEO_ANALYZER_UPDATED("Video Analyzer updated successfully", "VA-004"),
	VIDEO_ANALYZER_DELETED("Video Analyzer deleted successfully", "VA-005"),
	VIDEO_ANALYSIS_COMPLETED("Video Analysis completed successfully", "VA-006"),
	VIDEO_ANALYZER_LIST_FETCHED("Video Analyzer list fetched successfully", "VA-007"),

	// Error Messages for specific errors
	DUPLICATE_MEDIA_URL("Duplicate media URL found", "VA-008"),
	VIDEO_ANALYZER_ALREADY_EXISTS("Video Analyzer with the given media URL already exists", "VA-009"),
	ENTITY_NOT_FOUND("Entity not found", "VA-010"),
	ERROR_VIDEO_ANALYZER_FETCHED("Error while Video Analyzer fetched", "VA-011"), // Changed from VA-002 (duplicate)

	// Generic Success Responses
	FETCHED_LIST("List fetched successfully", "G-013"), // Changed from G-010 (duplicate)

	// Validation Errors
	VALIDATION_FAILED("Validation failed", "VA-013"),

	// Start Messages for Logging
	START_GET_VIDEO_ANALYZERS_LIST("Starting to fetch video analyzers list", "VA-100"),
	START_GET_VIDEO_ANALYZER("Starting to fetch video analyzer", "VA-101"),
	START_ADD_VIDEO_ANALYZER("Starting to add video analyzer", "VA-102"),
	START_UPDATE_VIDEO_ANALYZER("Starting to update video analyzer", "VA-103"),
	START_DELETE_VIDEO_ANALYZER("Starting to delete video analyzer", "VA-104"),
	START_VIDEO_ANALYSIS("Starting video analysis", "VA-105"),

	// App Version Error Codes
	NO_VERSION_FOUND("Update your app", "E-004"), 
	INVALID_DEVICE_INFO("Invalid device info", "E-005"),

	AUDIT_TRAIL_CREATED_SUCCESSFULLY("Audit trail created successfully.", "AUD-001"),
	NO_AUDIT_TRAIL_FOUND("No audit trail found with the given ID.", "AUD-002"),
	NO_AUDIT_TRAILS_FOUND_FOR_USER("No audit trails found for the given user ID.", "AUD-003"),
	NO_AUDIT_TRAILS_FOUND("No audit trails found.", "AUD-004"),
	FAILED_TO_CREATE_AUDIT_TRAIL("Failed to create audit trail.", "AUD-005"),
	FAILED_TO_RETRIEVE_AUDIT_TRAIL("Failed to retrieve audit trail.", "AUD-006"),
	FAILED_TO_RETRIEVE_AUDIT_TRAILS("Failed to retrieve audit trails.", "AUD-007"),
	AUDIT_TRAIL_FOUND_SUCCESSFULLY("Audit trail found successfully.", "AUD-008"),
	AUDIT_TRAILS_FOUND_SUCCESSFULLY("Audit trails found successfully.", "AUD-009"),
	AUDIT_TRAIL_EDITED_SUCCESSFULLY("Audit trail edited successfully.", "AUD-010"),
	FAILED_TO_EDIT_AUDIT_TRAIL("Failed to edit audit trail.", "AUD-011"),

	// Trial entries
	TRIAL_CREATED("Trial created successfully", "TR-001"), 
	TRIAL_UPDATED("Trial updated successfully", "TR-002"),
	TRIAL_DELETED("Trial deleted successfully", "TR-003"),
	TRIAL_LIST_FETCHED("Trials list fetched successfully", "TR-004"),
	TRIAL_FETCHED("Trial fetched successfully", "TR-005"),
	TRIAL_STATUS_UPDATED("Trial status updated successfully", "TR-006"),
	TRIAL_COMPLETED("Trial marked as completed", "TR-007"), 
	TRIAL_NOT_FOUND("Trial not found", "TR-008"),
	INVALID_TRIAL_DATA("Invalid trial data provided", "TR-009"), 
	ERROR_CREATING_TRIAL("Error creating trial", "TR-010"),
	ERROR_UPDATING_TRIAL("Error updating trial", "TR-011"), 
	ERROR_DELETING_TRIAL("Error deleting trial", "TR-012"),
	ERROR_FETCHING_TRIALS("Error fetching trials", "TR-013"), 
	ERROR_FETCHING_TRIAL("Error fetching trial", "TR-014"),
	ERROR_UPDATING_TRIAL_STATUS("Error updating trial status", "TR-015"),
	START_GET_TRIAL_LIST("Starting to fetch trail list", "TR-016"),
	INVALID_TRIAL_FEEDBACK_DATA("Invalid trial feedback data provided", "TR-017"),
	TRIAL_FEEDBACK_UPDATED("Trial feedback updated successfully", "TR-018"),
	ERROR_UPDATING_TRIAL_FEEDBAK("Error updating trial feedback", "TR-019"),

	// Activity Response
	ACTIVITY_CREATED("Activity created successfully", "ACT-001"),
	ACTIVITY_UPDATED("Activity updated successfully", "ACT-002"),
	ACTIVITY_DELETED("Activity deleted successfully", "ACT-003"),
	ACTIVITY_FETCHED("Activity fetched successfully", "ACT-004"), 
	ACTIVITY_NOT_FOUND("Activity not found", "ACT-005"),
	INVALID_ACTIVITY_DATA("Invalid activity data", "ACT-006"),
	ERROR_CREATING_ACTIVITY("Error creating activity", "ACT-007"),
	ERROR_UPDATING_ACTIVITY("Error updating activity", "ACT-008"),
	ERROR_DELETING_ACTIVITY("Error deleting activity", "ACT-009"),
	ERROR_FETCHING_ACTIVITY("Error fetching activity", "ACT-010"),
	START_GET_ACTIVITY_LIST("Starting to get activity list", "ACT-011"),
	DUPLICATE_ACTIVITY_NAME("Activity with same name already exists", "ACT-012"),

	// Schedule Response
	SCHEDULE_ADDED("Schedule created successfully", "S-001"),
	SCHEDULE_UPDATED("Schedule updated successfully", "S-002"),
	SCHEDULE_DELETED("Schedule deleted successfully", "S-003"),
	SCHEDULE_FETCHED("Schedule fetched successfully", "S-004"), 
	SCHEDULE_NOT_FOUND("Schedule not found", "S-005"),
	INVALID_SCHEDULE_REQUEST("Invalid schedule data", "S-006"),
	EXCEPTION_IN_ADD_SCHEDULE("Error creating schedule", "S-007"),
	EXCEPTION_IN_UPDATE_SCHEDULE("Error updating schedule", "S-008"),
	EXCEPTION_IN_DELETE_SCHEDULE("Error deleting schedule", "S-009"),
	ERROR_SCHEDULE_FETCHED("Error fetching schedule", "S-010"), 
	START_GET_SCHEDULE("Starting to get schedule", "S-011"),
	START_ADD_SCHEDULE("Starting to add schedule", "S-012"),
	START_UPDATE_SCHEDULE("Starting to update schedule", "S-013"),
	START_DELETE_SCHEDULE("Starting to delete schedule", "S-014"),
	START_GET_SCHEDULES_LIST("Starting to get schedules list", "S-015"),
	SCHEDULE_FETCHED_LIST("Schedule list fetched successfully", "S-016"),
	ERROR_FETCHING_SCHEDULE_LIST("Error fetching schedule list", "S-017"),
	COURSE_NOT_FOUND("Course not found", "S-018"),
	DUPLICATE_SCHEDULE_NAME("Schedule with same name already exists", "S-019"),
	DAY_ACTIVITY_ADDED("Daywise activity added successfully", "S-020"),
	DAY_ACTIVITY_UPDATED("Daywise activity updated successfully", "S-021"),
	DAY_ACTIVITY_DELETED("Daywise activity deleted successfully", "S-022"),
	DAY_ACTIVITY_NOT_FOUND("Daywise activity not found", "S-023"),
	DAY_TIMESLOT_ADDED("Daywise timeslot added successfully", "S-024"),
	DAY_TIMESLOT_UPDATED("Daywise timeslot updated successfully", "S-025"),
	DAY_TIMESLOT_DELETED("Daywise timeslot deleted successfully", "S-026"),
	DAY_TIMESLOT_NOT_FOUND("Daywise timeslot not found", "S-027"),

	MASTER_SUCCESS("Master data fetched successfully", "MD-001"),
	ERROR_MASTER_EXPORTING_DATA("Error occurred while fetching master data", "MD-002"),
	MASTER_DATA_NOT_FOUND("Master data is not found", "MD-003"),

	EXPORT_SUCCESS("Data exported successfully", "EX-001"),
	ERROR_EXPORTING_DATA("Error occurred while exporting data to Excel", "EX-002"),
	UPLOAD_INITIATED("Upload Initiated", "BU-001"),

	// Bulk Upload History Specific Responses
	BULK_UPLOAD_HISTORY_NOT_FOUND("Bulk upload history not found", "BH-001"),
	BULK_UPLOAD_HISTORY_FETCHED("Bulk upload history fetched successfully", "BH-002"),
	ERROR_BULK_UPLOAD_HISTORY_FETCHED("Error while fetching bulk upload history", "BH-003"),

	// Start Messages for Logging
	START_GET_BULK_UPLOAD_HISTORY_LIST("Starting to fetch bulk upload history list", "BH-100"),
	START_GET_BULK_UPLOAD_HISTORY("Starting to fetch bulk upload history", "BH-101"),
	CERTIFICATE_SAVED_SUCCESS("Certificate saved successfully", "CH-001"),
	INVALID_DATE_FORMAT("Invalid date format", "U-123"), // Changed from U-110 (duplicate)
	PAYMENT_DETAILS_NOT_FOUND("Payment details not found", "PAY-404");

	static {
		Set<String> lookup = new HashSet<>();
		for (ApiResponse response : ApiResponse.values()) {
			if (lookup.contains(response.getStatusCode())) {
				log.error(String.format("Duplicate code: %s configured. Last instance will be used",
						response.getStatusCode()));
			} else {
				lookup.add(response.getStatusCode());
			}
		}
	}
	
	public final String message;
	private final String statusCode;

	ApiResponse(String message, String statusCode) {
		this.message = message;
		this.statusCode = statusCode;
	}

	public String getMessage() {
		return message;
	}

	public String getStatusCode() {
		return statusCode;
	}
}