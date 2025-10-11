package com.playmotech.api.core.helper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.validation.VideoAnalyzerValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2

@Component

@RequiredArgsConstructor

public class UserProfileHelper {

	private final UserProfileRepo userProfileRepository;

	private final VideoAnalyzerValidation validation;

	public ServiceResponse fetchUserProfileById(String userId) {

		log.info(ApiResponse.VALIDATING_ID.getMessage());

		if (!validation.isValidId(userId)) {

			log.warn(ApiResponse.INVALID_USER_ID.getMessage());

			return ResponseBuilder.badRequest(ApiResponse.INVALID_USER_ID);

		}

		Optional<UserProfile> userProfile = userProfileRepository.findByIdAndInactiveIsFalse(userId);

		return userProfile.map(user -> {

			log.info(ApiResponse.USER_FETCHED.getMessage());

			return ResponseBuilder.success(user, ApiResponse.USER_FETCHED, HttpStatus.OK);

		}).orElseGet(() -> {

			log.warn(ApiResponse.USER_NOT_FOUND.getMessage());

			return ResponseBuilder.notFound(ApiResponse.USER_NOT_FOUND);

		});

	}

	/**
	 * Validates both display name and email uniqueness
	 * 
	 * @param phoneNumber   The phone number
	 * @param displayName   The display name to validate
	 * @param emailId       The email to validate
	 * @param excludeUserId User ID to exclude from validation (for updates)
	 * @throws ResourceException if validation fails
	 */
	public void validateUserProfile(String phoneNumber, String displayName, String emailId, String excludeUserId)
			throws ResourceException {
		validateDisplayNameForPhoneNumber(phoneNumber, displayName, excludeUserId);
		validateEmailUniqueness(emailId, phoneNumber, excludeUserId);
	}

	/**
	 * Validates that display name is unique for accounts with the same phone number
	 * 
	 * @param phoneNumber   The phone number to check
	 * @param displayName   The display name to validate
	 * @param excludeUserId User ID to exclude from validation (for updates)
	 * @throws ResourceException if validation fails
	 */
	public void validateDisplayNameForPhoneNumber(String phoneNumber, String displayName, String excludeUserId)
			throws ResourceException {
		if (!StringUtils.hasText(displayName) || !StringUtils.hasText(phoneNumber)) {
			return;
		}

		boolean displayNameExists = checkDisplayNameExists(phoneNumber, displayName, excludeUserId);

		if (displayNameExists) {
			throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT, "Display name '" + displayName
					+ "' already exists for phone number " + phoneNumber + ". Please choose a different display name.");
		}
	}

	/**
	 * Validates that email is unique across different phone numbers
	 * 
	 * @param emailId       The email to validate
	 * @param phoneNumber   The phone number of the current user
	 * @param excludeUserId User ID to exclude from validation (for updates)
	 * @throws ResourceException if validation fails
	 */
	public void validateEmailUniqueness(String emailId, String phoneNumber, String excludeUserId)
			throws ResourceException {
		if (!StringUtils.hasText(emailId)) {
			return; // Email is optional
		}

		boolean emailExistsWithDifferentPhone = checkEmailExistsWithDifferentPhone(emailId, phoneNumber, excludeUserId);

		if (emailExistsWithDifferentPhone) {
			throw new ResourceException(ErrorCodes.RESOURCE_CONFLICT,
					"Email '" + emailId + "' is already associated with a different phone number. "
							+ "Each email can only be used with one phone number.");
		}
	}

	/**
	 * Checks if display name exists for a phone number (excluding specific user if
	 * provided)
	 */
	private boolean checkDisplayNameExists(String phoneNumber, String displayName, String excludeUserId) {
		if (excludeUserId != null) {
			// For updates - use repository method that excludes current user
			return userProfileRepository.existsByPhoneNumberAndDisplayNameIgnoreCaseAndInactiveAndIdNot(phoneNumber,
					displayName, false, excludeUserId);
		} else {
			// For new users - check all active profiles with same phone and display name
			List<UserProfile> existingProfiles = userProfileRepository
					.findByPhoneNumberAndDisplayNameIgnoreCaseAndInactive(phoneNumber, displayName, false);
			return !existingProfiles.isEmpty();
		}
	}

	/**
	 * Checks if email exists with a different phone number (excluding specific user
	 * if provided)
	 */
	private boolean checkEmailExistsWithDifferentPhone(String emailId, String phoneNumber, String excludeUserId) {
		if (excludeUserId != null) {
			// For updates - use repository method that excludes current user
			return userProfileRepository.existsByEmailIdIgnoreCaseAndPhoneNumberNotAndInactiveAndIdNot(emailId,
					phoneNumber, false, excludeUserId);
		} else {
			// For new users - find all users with same email and check if any has different
			// phone
			List<UserProfile> usersWithSameEmail = userProfileRepository.findByEmailIdIgnoreCaseAndInactive(emailId,
					false);
			return usersWithSameEmail.stream()
					.anyMatch(profile -> !Objects.equals(phoneNumber, profile.getPhoneNumber()));
		}
	}

	/**
	 * Validates user profile with detailed error information Returns a formatted
	 * error message with all validation issues
	 * 
	 * @param phoneNumber   The phone number
	 * @param displayName   The display name to validate
	 * @param emailId       The email to validate
	 * @param excludeUserId User ID to exclude from validation (for updates)
	 * @return ValidationResult containing success status and error message
	 */
	public ValidationResult validateWithDetailedErrors(String phoneNumber, String displayName, String emailId,
			String excludeUserId) {
		StringBuilder errors = new StringBuilder();

		// Check display name
		if (StringUtils.hasText(displayName) && StringUtils.hasText(phoneNumber)) {
			if (checkDisplayNameExists(phoneNumber, displayName, excludeUserId)) {
				errors.append("Display name '").append(displayName).append("' already exists for phone number ")
						.append(phoneNumber).append(". ");
			}
		}

		// Check email
		if (StringUtils.hasText(emailId)) {
			if (checkEmailExistsWithDifferentPhone(emailId, phoneNumber, excludeUserId)) {
				errors.append("Email '").append(emailId)
						.append("' is already associated with a different phone number. ");
			}
		}

		boolean isValid = errors.length() == 0;
		return new ValidationResult(isValid, errors.toString().trim());
	}

	/**
	 * Inner class to hold validation results
	 */
	public static class ValidationResult {
		private final boolean valid;
		private final String errorMessage;

		public ValidationResult(boolean valid, String errorMessage) {
			this.valid = valid;
			this.errorMessage = errorMessage;
		}

		public boolean isValid() {
			return valid;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public boolean hasErrors() {
			return !valid;
		}
	}

}
