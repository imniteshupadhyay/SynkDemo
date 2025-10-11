package com.playmotech.api.core.dao_postgres;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UnitType;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig.ParameterType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "assessment_player_submission_parameter")
public class AssessmentPlayerSubmissionParameter {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "submission_param_id")
	private String submissionParamId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "submission_id", referencedColumnName = "submission_id", nullable = false)
	private AssessmentPlayerSubmission submission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_config_id", referencedColumnName = "parameter_id", nullable = false)
	private AssessmentParameterConfig parameterConfig;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_id", referencedColumnName = "parameter_id", nullable = false)
	private AssessmentParameter parameter;

	@Enumerated(EnumType.STRING)
	@Column(name = "sport")
	private Sports sport;

	@Enumerated(EnumType.STRING)
	@Column(name = "age_category")
	private AgeCategory ageCategory;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender")
	private Gender gender;

	@Column(name = "parameter_name")
	private String parameterName;

	@Column(name = "parameter_weight")
	private Double parameterWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "parameter_type", nullable = false)
	private ParameterType parameterType;

	@Enumerated(EnumType.STRING)
	@Column(name = "unit_type", nullable = false)
	private UnitType unitType;

	@Enumerated(EnumType.STRING)
	@Column(name = "comparison_type", nullable = false)
	private ComparisonType comparisonType;

	@Column(name = "submitted_value", nullable = false)
	private String submittedValue;

	@Column(name = "video_url", columnDefinition = "TEXT")
	private String videoUrl;

	@Column(name = "score")
	private Double score;

	@Column(name = "global_score")
	private Double globalScore;

}
