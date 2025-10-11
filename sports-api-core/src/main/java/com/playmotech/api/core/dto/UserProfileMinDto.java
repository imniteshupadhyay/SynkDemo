package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UserType;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class UserProfileMinDto {
	private String id;
	private String displayName;
	private UserType userType;
	private String profilePictureUrl;
	private Integer experienceInMonths;
	private Map<Sports, SkillLevel> expertiseLevel;
	private String phoneNumber;
	private boolean primaryAccount;
	private String dob;
}