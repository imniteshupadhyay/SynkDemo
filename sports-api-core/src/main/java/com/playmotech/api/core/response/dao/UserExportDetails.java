package com.playmotech.api.core.response.dao;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Role;
import com.playmotech.api.core.constants.UserType;

import lombok.Data;

@Data
public class UserExportDetails {

	private String id;
	private String displayName;
	private String designation;
	private String dob;
	private String emailId;
	private String phoneNumber;
	private Gender gender;
	private Integer experienceInMonths;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	private UserType userType;
	private Role role;

}
