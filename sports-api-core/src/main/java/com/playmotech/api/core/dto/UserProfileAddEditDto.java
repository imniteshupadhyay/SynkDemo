package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.Roles;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserProfileAddEditDto {

	private String id;

	// username is phoneNumber
	private String username;

	private String password;

	// displayName is fullName
	@NotEmpty(message = "Display Name cannot be empty.")
	private String displayName;

	// @NotNull(message = "Email ID is mandatory.")
	// @NotEmpty(message = "Email ID cannot be empty.")
	private String emailId;

	@NotEmpty(message = "Phone number cannot be empty.")
	// TODO: Temporarily removed the size limit
	// @Size(min = 12, max = 12, message = "Phone number should be 10-digit")
	@Pattern(regexp = "[0-9]+", message = "Phone numbers can only contain digits.")
	private String phoneNumber;

	private String profilePictureUrl;

	private String pincode;

	private String city;

	private String state;

	private String country;

	private String addressLine1;

	private String addressLine2;

	private Integer experienceInMonths;

	private Roles role;

	private String dob;

	private Gender gender;

	private UserType userType;

	private List<Sports> preferredSports;

	private Map<Sports, SkillLevel> expertiseLevel;

	private UserDocumentsDto uploadedDocuments;

	private String userActions;

	private String designation;

	private List<String> academyId;

	private List<CoachAcademyDetails> academyCoaches;

	private boolean primaryAccount;
}
