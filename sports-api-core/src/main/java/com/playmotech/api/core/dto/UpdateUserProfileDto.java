package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class UpdateUserProfileDto {
	private String displayName;
	private UserType userType;
	private String googleAdvertisingId;
	private String emailId;
	private Gender gender;
	private Set<Sports> preferredSports;
	private Map<Sports, SkillLevel> expertiseLevel;
	private String androidFcmPushToken;
	private Integer experienceInMonths;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	private Role role;
	private Long roleId;
	private String designation;
	private List<String> academyId;
	private String dob;
}
