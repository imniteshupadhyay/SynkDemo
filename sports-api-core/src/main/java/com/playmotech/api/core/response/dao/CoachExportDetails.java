package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class CoachExportDetails {

	private String name;
	private String email;
	private String phoneNumber;
	private Integer age;
	private String designation;
	private String academyNames;
	private String academyDesignations;
	private String role;
	private String qualification;
	private String gender;
	private LocalDate dob;
	private String aadharCard;
	private String panCard;
	private List<String> associatedPrograms;
	private List<String> sports;
	private String status;

}
