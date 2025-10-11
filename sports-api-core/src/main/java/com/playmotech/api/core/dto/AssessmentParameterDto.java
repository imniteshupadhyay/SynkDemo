package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.UnitType;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig.ParameterType;

import lombok.Data;

@Data
public class AssessmentParameterDto {

	private String parameterId;
	private String parameterConfigId; // Reference to the parameter configuration
	private String parameterName;
	private String parameterDescription;
	private Double parameterWeight;

	private AgeCategory ageCategory;
	private Gender gender;

	private ParameterType parameterType;
	private UnitType unitType;
	private ComparisonType comparisonType;

	private Boolean videoUploadEnabled;

}
