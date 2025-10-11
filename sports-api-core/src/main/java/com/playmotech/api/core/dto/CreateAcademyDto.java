package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class CreateAcademyDto {
	private String id;
	private String name;
	private String emailId;
	private String phoneNumber;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	private List<Sports> sports;
	private String iconUrl;
	private String ownerName;
	private Gender ownerGender;
	private String dob;
	private String startTime;
	private String endTime;
	private String ownerUserId;
	private Integer roleId;
	private String orgId;
}
