package com.playmotech.api.core.response.dao;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class TraineeViewDao {

	private String playerName;

	private String dob;

	private String emailId;

	private String phoneNumber;

	private String gender;

	private Timestamp createdOn;

	private String addressLine1;

	private String addressLine2;

	private String pincode;

	private String city;

	private String state;

	private String country;

	private String programNames;

}
