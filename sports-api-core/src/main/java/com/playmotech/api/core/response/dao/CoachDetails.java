package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class CoachDetails {

	private String id;
	private String name;
	private String email;
	private String username;
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
	private String academyIds;
	private String status;
	private String academyJson; // This will store JSON as a string
	private LocalDateTime createdOn;

	// Helper method to handle null values if needed
	public String getAcademyJson() {
		return academyJson != null ? academyJson : "[]";
	}
}