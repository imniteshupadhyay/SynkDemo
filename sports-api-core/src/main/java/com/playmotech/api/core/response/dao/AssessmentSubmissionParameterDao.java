package com.playmotech.api.core.response.dao;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UnitType;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig.ParameterType;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSubmissionParameterDao {

	private String submissionParamId;
	private String submissionId;
	private String parameterId;

	private String parameterName;
	private Double parameterWeight;

	private Sports sport;
	private AgeCategory ageCategory;
	private Gender gender;

	private ParameterType parameterType;
	private UnitType unitType;
	private ComparisonType comparisonType;

	private String submittedValue;
	private String videoUrl;

	private Double score;
	private Double globalScore;
}
