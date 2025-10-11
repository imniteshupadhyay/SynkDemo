package com.playmotech.api.core.views;

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
@Table(name = "trainee_performance_report_view")
public class TraineePerformanceReportView {

	@Id
	@Column(name = "report_id")
	private String reportId;

	@Column(name = "player_id")
	private String playerId;

	@Column(name = "report_title")
	private String reportTitle;

	@Column(name = "report_json")
	private String reportJson;

	@Column(name = "coach_id")
	private String coachId;

	@Column(name = "coach_name")
	private String coachName;

	@Column(name = "player_name")
	private String playerName;

	@Column(name = "sport")
	private String sport;

	@Column(name = "report_status")
	private String reportStatus;

	@Column(name = "created_on")
	private LocalDateTime createdOn;

	@Column(name = "academy")
	private String academy;

	@Column(name = "branch")
	private String branch;

	@Column(name = "program")
	private String program;

	@Column(name = "program_min_age")
	private Integer programMinAge;

	@Column(name = "program_max_age")
	private Integer programMaxAge;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "branch_id")
	private String branchId;

	@Column(name = "program_id")
	private String programId;

	@Column(name = "age_category")
	private String ageCategory;

	@Column(name = "enroll_id")
	private String enrollId;

	@Column(name = "coach_ids")
	private List<String> coachIds;

	@Column(name = "maintainer_ids")
	private List<String> maintainerIds;

	@Column(name = "manager_id")
	private String managerId;

	@Column(name = "domain_url")
	private String domainUrl;
}
