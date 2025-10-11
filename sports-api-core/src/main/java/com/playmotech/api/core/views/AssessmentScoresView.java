package com.playmotech.api.core.views;

import org.springframework.data.annotation.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Immutable
@Table(name = "assessment_scores_view")
public class AssessmentScoresView {

	@Id
	@Column(name = "registration_id")
	private String registrationId;

	@Column(name = "registration_number")
	private String registrationNumber;

	@Column(name = "player_id")
	private String playerId;

	@Column(name = "display_name")
	private String displayName;

	@Column(name = "academy_id")
	private String academyId;

	@Column(name = "academy_name")
	private String academyName;

	@Column(name = "assessment_id")
	private String assessmentId;

	@Column(name = "sport")
	private String sport;

	@Column(name = "gender")
	private String gender;

	@Column(name = "age_category")
	private String ageCategory;

	@Column(name = "total_score")
	private Double totalScore;

	@Column(name = "assessment_rank")
	private Integer assessmentRank;

}
