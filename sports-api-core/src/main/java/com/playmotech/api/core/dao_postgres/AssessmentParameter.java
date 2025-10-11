package com.playmotech.api.core.dao_postgres;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "assessment_parameter")
public class AssessmentParameter {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "parameter_id")
	private String parameterId;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assessment_id", referencedColumnName = "id", nullable = false)
	private Assessment assessment;

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

	@Column(name = "parameter_description")
	private String parameterDescription;

	@Column(name = "parameter_weight")
	private Double parameterWeight;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_config_id", referencedColumnName = "parameter_id")
	private AssessmentParameterConfig parameterConfig;

	@Enumerated(EnumType.STRING)
	@Column(name = "parameter_type", nullable = false)
	private ParameterType parameterType;

	@Enumerated(EnumType.STRING)
	@Column(name = "unit_type", nullable = false)
	private UnitType unitType;

	@Enumerated(EnumType.STRING)
	@Column(name = "comparison_type", nullable = false)
	private ComparisonType comparisonType;

	@Column(name = "video_upload_enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean videoUploadEnabled;

}
