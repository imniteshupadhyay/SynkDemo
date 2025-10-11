package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Entity
@Table(name = "course_attendances")
@Data
public class CourseAttendance {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;
	@Column(name = "normalized_date")
	private Long normalizedDate;
	@Column(name = "attendance_json")
	private String attendanceJson;
	@Column(name = "created_on")
	private Timestamp createdOn;
	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@ManyToOne
	@JoinColumn(name = "created_by_user_id", referencedColumnName = "id")
	private UserProfile createdByUserProfile;
}
