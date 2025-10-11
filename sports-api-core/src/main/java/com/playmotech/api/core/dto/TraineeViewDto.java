package com.playmotech.api.core.dto;

import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraineeViewDto {
	private String id;
	private String username;
	private String displayName;
	private String dob;
	private String emailId;
	private String phoneNumber;
	private String profilePictureUrl;
	private String aboutMe;
	private String gender;
	private Timestamp createdOn;
	private Boolean inactive;
	private Boolean isPhoneNumberVerified;
	private Boolean isEmailIdVerified;
	private Integer experienceInMonths;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	private String role;
	private List<String> academyId;
	private List<String> academyName;
	private String domainUrl;
	private List<String> status;
}
