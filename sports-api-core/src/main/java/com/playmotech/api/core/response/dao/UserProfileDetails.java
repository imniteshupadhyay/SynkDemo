package com.playmotech.api.core.response.dao;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dto.CoachAcademyDto;
import com.playmotech.api.core.dto.PlayerEnrollInCourseDto;

import lombok.Data;

@Data
public class UserProfileDetails {

	private String id;
	private String username;
	private String password;
	private String displayName;
	private String dob;
	private String emailId;
	private String phoneNumber;
	private String profilePictureUrl;
	private Gender gender;
	private boolean isPhoneNumberVerified;
	private boolean isEmailIdVerified;
	private Integer experienceInMonths;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	private Role role;
	private Long roleId;
	private UserType userType;
	private List<String> academyId;
	private List<CoachAcademyDto> academyCoaches;

	private Map<String, String> academyDesignationMap; // academyId -> designation
	private Map<Sports, SkillLevel> expertiseLevel; // sport -> skill level
	private List<PlayerEnrollInCourseDto> enrollInAcademies;
	private String aadharUrl;
	private String panUrl;

}
