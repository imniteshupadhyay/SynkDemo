package com.playmotech.api.core.views;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Immutable
@Table(name = "coach_attendance_view")
public class CoachAttendanceView {

	@Id
	@Column(name = "unique_id")
	private Long id;

	@Column(name = "coach_name")
	private String coachName;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "email_id")
	private String email;

	@Column(name = "coach_id")
	private String coachId;

	@Column(name = "attendance_date")
	private LocalDate attendanceDate;

	@Column(name = "status")
	private String status;

	@Column(name = "check_in_time")
	private LocalDateTime checkInTime;

	@Column(name = "check_out_time")
	private LocalDateTime checkOutTime;

	@Column(name = "academy")
	private String academy;

	@Column(name = "sport")
	private String sport;

	@Column(name = "program")
	private String program;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "program_id")
	private String programId;

	@Column(name = "age_category")
	private String ageCategory;

	@Column(name = "check_in_latitude")
	private Double checkInLatitude;

	@Column(name = "check_in_longitude")
	private Double checkInLongitude;

	@Column(name = "check_in_distance_meters")
	private Double checkInDistanceMeters;

	@Column(name = "check_out_latitude")
	private Double checkOutLatitude;

	@Column(name = "check_out_longitude")
	private Double checkOutLongitude;

	@Column(name = "check_out_distance_meters")
	private Double checkOutDistanceMeters;

	@Column(name = "auto_checked_out")
	private Boolean autoCheckedOut;

	@Column(name = "record_created_at")
	private LocalDateTime recordCreatedAt;

	@Column(name = "record_updated_at")
	private LocalDateTime recordUpdatedAt;

	@Column(name = "coach_ids", columnDefinition = "text[]")
	private List<String> coachIds;

	@Column(name = "maintainer_ids", columnDefinition = "text[]")
	private List<String> maintainerIds;

	@Column(name = "manager_id")
	private String managerId;

	@Column(name = "domain_url")
	private String domainUrl;

	// 🔹 Geo Fence fields
	@Column(name = "geo_fence_id")
	private Long geoFenceId;

	@Column(name = "geo_fence_name")
	private String geoFenceName;

	@Column(name = "geo_fence_latitude")
	private Double geoFenceLatitude;

	@Column(name = "geo_fence_longitude")
	private Double geoFenceLongitude;

	@Column(name = "geo_fence_radius")
	private Long geoFenceRadius;

	@Column(name = "geo_fence_user_type")
	private String geoFenceUserType;

	@Column(name = "geo_fence_send_notification")
	private Boolean geoFenceSendNotification;

}
