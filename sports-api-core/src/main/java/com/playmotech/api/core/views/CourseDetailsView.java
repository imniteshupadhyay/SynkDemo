package com.playmotech.api.core.views;

import java.util.List;

import org.springframework.data.annotation.Immutable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playmotech.api.core.dao_postgres.ScheduleFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Table(name = "course_details_view")
public class CourseDetailsView {

	@Id
	@Column(name = "course_id")
	private String courseId;

	private String title;

	private String description;

	private String sport;

	@Column(name = "coach_user_ids")
	private List<String> coachUserIds;

	@Column(name = "maintainer_ids")
	private List<String> maintainerIds;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "age_category")
	private String ageCategory;

	@Column(name = "domain_url")
	private String domainUrl;

	@Column(name = "manager_user_id")
	private String managerUserId;

	@Column(name = "schedule_type")
	private String scheduleType;

	@Column(name = "custom_dates_json")
	private String customDatesJson;

	@Column(name = "weekdays_json")
	private String weekdaysJson;

	@Column(name = "start_date")
	private String startDate;

	@Column(name = "end_date")
	private String endDate;

	private String createdOn;

	@Column(name = "schedule_file_id")
	@JsonIgnore
	private Long scheduleFileId;

	@Transient
	private ScheduleFile scheduleFile;

	@JsonIgnore
	@Transient
	private boolean completed;

}
