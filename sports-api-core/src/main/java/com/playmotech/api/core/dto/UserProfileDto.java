package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class UserProfileDto {
	private String id;
	private String username;
	private String password;
	@Size(min = 2, max = 254, message = "Name must be between 2 and 254 characters.")
	@NotEmpty(message = "Cannot be empty.")
	private String displayName;
	private String dob;
	private UserType userType;
	private ProfileStats profileStats;
	private String googleAdvertisingId;
	private String emailId;
	@NotEmpty(message = "Cannot be empty.")
	// TODO: Temporarily removed the size limit
	// @Size(min = 12, max = 12, message = "Phone number should be 10-digit")
	@Pattern(regexp = "[0-9]+", message = "Phone numbers can only contain digits.")
	private String phoneNumber;
	private String profilePictureUrl;
	private Gender gender;
	private Set<Sports> preferredSports;
	private boolean isPhoneNumberVerified;
	private boolean isEmailIdVerified;
	private Map<Sports, SkillLevel> expertiseLevel;
	private Integer experienceInMonths;
	private String androidFcmPushToken;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	private List<VisibilityConfigDto> visibilityConfig;
	private Role role;
	private Long roleId;
	private List<String> academyId;
	private String designation; // coach's designation
	private boolean primaryAccount;
}
