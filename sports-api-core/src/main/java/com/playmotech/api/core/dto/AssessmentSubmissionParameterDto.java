package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UnitType;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig.ParameterType;

import lombok.Data;

@Data
public class AssessmentSubmissionParameterDto {

	private String submissionParamId;
	private String submissionId;
	private String parameterConfigId;
	private String parameterId;
	private Sports sport;
	private AgeCategory ageCategory;
	private Gender gender;
	private String parameterName;
	private Double parameterWeight;
	private ParameterType parameterType;
	private UnitType unitType;
	private ComparisonType comparisonType;
	private String submittedValue;
	private String videoUrl;

}
