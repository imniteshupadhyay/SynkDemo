package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.playmotech.api.core.constants.SkillLevel;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "courses")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@ToString(exclude = { "academy", "coachUserProfile", "courseCoachMappings", "paymentOptions", "ruleAndRegulations",
		"schedule" })
@EqualsAndHashCode(exclude = { "academy", "coachUserProfile", "courseCoachMappings", "paymentOptions",
		"ruleAndRegulations", "schedule" })
public class Course {
	@Id
	private String id;

	@Column(name = "created_on")
	private Timestamp createdOn;

	@Column(name = "title")
	private String title;

	@Column(name = "description")
	private String description;

	@ManyToOne
	@JoinColumn(name = "coach_user_id", referencedColumnName = "id")
	private UserProfile coachUserProfile;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;
	@Column(name = "level")
	@Enumerated(value = EnumType.STRING)
	private SkillLevel level;
	@Column(name = "total_max_trainees")
	private Long totalMaxTrainees;
	@Column(name = "min_age")
	private Long minAge;
	@Column(name = "max_age")
	private Long maxAge;

	@JsonManagedReference
	@OneToOne(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Schedule schedule;

	@Column(name = "icon_url")
	private String iconUrl;
	@Column(name = "inactive")
	private Boolean inactive;
	@Column(name = "sport")
	@Enumerated(value = EnumType.STRING)
	private Sports sport;

	@Column(name = "visibility")
	@Enumerated(value = EnumType.STRING)
	private Visibility visibility;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "course", cascade = CascadeType.ALL)
	@JsonManagedReference
	private List<CourseCoachMapping> courseCoachMappings;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "course", cascade = CascadeType.ALL)
	@JsonManagedReference
	private List<CourseRuleAndRegulationsMapping> ruleAndRegulations;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "course", cascade = CascadeType.ALL)
	@JsonManagedReference
	private List<CoursePaymentOptionsMapping> paymentOptions;

	@Column(name = "age_category")
	private String ageGroup;

	@Column(name = "registration_fee")
	private Long registrationFee;

	@ManyToOne
	@JoinColumn(name = "schedule_file_id", referencedColumnName = "id")
	private ScheduleFile scheduleFile;
}
