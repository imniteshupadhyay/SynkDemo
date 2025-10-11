package com.playmotech.api.core.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class TraineeAttendanceDto {
	private String traineeUserId;
	private String name;
	private String phoneNumber;
	private Boolean attended;
	private String startTime;
	private String endTime;
	private String traineeProfilePic;
	private Integer totalSessions;
	private Integer attendedSessions;
	private LocalDate joiningDate;
}