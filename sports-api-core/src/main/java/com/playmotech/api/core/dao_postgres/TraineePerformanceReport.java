package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "trainee_performance_reports")
@Data
public class TraineePerformanceReport {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "trainee_user_id", referencedColumnName = "id")
	private UserProfile traineeUserProfile;
	@Column(name = "title")
	private String title;

	@ManyToOne
	@JoinColumn(name = "coach_user_id", referencedColumnName = "id")
	private UserProfile coachUserProfile;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;
	@Column(name = "sport")
	@Enumerated(value = EnumType.STRING)
	private Sports sport;
	@Column(name = "created_on")
	private Timestamp createdOn;
	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<TraineePerformanceReportMediaMapping> mediaMappings;
	@Column(name = "status")
	@Enumerated(value = EnumType.STRING)
	private PerformanceReportStatus status;
	@Column(name = "report_json")
	private String reportJson;
}
