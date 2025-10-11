package com.playmotech.api.core.views;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Immutable
@Table(name = "attendance_view")
public class AttendanceView {

	@Id
	@Column(name = "unique_id")
	private String uniqueId;

	@Column(name = "player_id")
	private String playerId;

	@Column(name = "player_name")
	private String playerName;

	@Column(name = "status")
	private String status;

	@Column(name = "academy")
	private String academy;

	@Column(name = "branch")
	private String branch;

	@Column(name = "sport")
	private String sport;

	@Column(name = "program")
	private String program;

	@Column(name = "program_min_age")
	private Integer programMinAge;

	@Column(name = "program_max_age")
	private Integer programMaxAge;

	@Column(name = "marked_by")
	private String markedBy;

	@Column(name = "marked_by_id")
	private String markedById;

	@Column(name = "marked_at")
	private LocalDateTime markedAt;

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

	@Column(name = "normalized_date_converted")
	private LocalDate normalizedDate;

	@Transient
	@JsonIgnore
	private Long days;
}
