package com.playmotech.api.core.response.dao;

import java.util.List;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dto.PlayerEnrollInCourseDto;

import lombok.Data;

@Data
public class UserAddEditDao {

	private String id;
	// username is phoneNumber
	private String username;
	private String displayName;
	private String emailId;
	private String phoneNumber;

	private String profilePictureUrl;

	private String pincode;

	private String city;

	private String state;

	private String country;

	private String addressLine1;

	private String addressLine2;

	private Integer experienceInMonths;

	private String dob;

	private Gender gender;

	private UserType userType;

	private List<PlayerEnrollInCourseDto> enrollInAcademies;

}
