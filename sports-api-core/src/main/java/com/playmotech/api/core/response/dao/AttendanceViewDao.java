package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AttendanceViewDao {

//	private String uniqueId;

	private String playerName;

	private String status;

	private String academy;

//	private String branch;

	private String sport;

	private String program;

	private String markedBy;

	private LocalDateTime markedAt;

	private String ageCategory;

//	private String enrollId;
}
