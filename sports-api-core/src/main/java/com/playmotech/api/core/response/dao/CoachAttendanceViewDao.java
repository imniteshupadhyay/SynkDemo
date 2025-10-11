package com.playmotech.api.core.response.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.playmotech.api.core.utils.ColumnHeader;

import lombok.Data;

@Data
public class CoachAttendanceViewDao {

	@ColumnHeader(value = "Academy", order = 1)
	private String academy;

	@ColumnHeader(value = "Program", order = 2)
	private String program;

	@ColumnHeader(value = "Coach Name", order = 3)
	private String coachName;

	@ColumnHeader(value = "Phone Number", order = 4)
	private String phoneNumber;

	@ColumnHeader(value = "Email", order = 5)
	private String email;

	@ColumnHeader(value = "Attendance Date", order = 6, format = "dd-MMM-yyyy")
	private LocalDate attendanceDate;

	@ColumnHeader(value = "Status", order = 7)
	private String status;

	@ColumnHeader(value = "Check-In Time", order = 8, format = "dd-MMM-yyyy hh:mm a")
	private LocalDateTime checkInTime;

	@ColumnHeader(value = "Check-Out Time", order = 9, format = "dd-MMM-yyyy hh:mm a")
	private LocalDateTime checkOutTime;

	@ColumnHeader(value = "Check-In Latitude", order = 10, include = false)
	private Double checkInLatitude;

	@ColumnHeader(value = "Check-In Longitude", order = 11, include = false)
	private Double checkInLongitude;

	@ColumnHeader(value = "Check-In Distance (m)", order = 12)
	private Double checkInDistanceMeters;

	@ColumnHeader(value = "Check-Out Latitude", order = 13, include = false)
	private Double checkOutLatitude;

	@ColumnHeader(value = "Check-Out Longitude", order = 14, include = false)
	private Double checkOutLongitude;

	@ColumnHeader(value = "Check-Out Distance (m)", order = 15)
	private Double checkOutDistanceMeters;

	// 👉 Skip audit fields
	@ColumnHeader(value = "Record Created At", order = 16, format = "dd-MMM-yyyy hh:mm a")
	private LocalDateTime recordCreatedAt;

	@ColumnHeader(value = "Record Updated At", order = 17, include = false, format = "dd-MMM-yyyy hh:mm a")
	private LocalDateTime recordUpdatedAt;
}
