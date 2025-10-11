package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class TrialExportDao {
	private String name;
	private String address;
	private LocalDate trailDate;
	private LocalDate dob;
	private LocalTime trailTime;
	private String gender;
	private String status;
	private String sports;
	private String email;
	private String phone;
	private String reason;
	private String coach;
	private String academy;
	private String createdBy;
	private String updatedBy;
	private boolean completed;
	private LocalDateTime insertedOn;
	private LocalDateTime updatedOn;
}
