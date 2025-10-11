package com.playmotech.api.core.response.dao;

import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UnitType;
import com.playmotech.api.core.dao_postgres.AssessmentParameterConfig.ParameterType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentParameterConfigDao {

	private String parameterId;

	private String parameterName;
	private String parameterDescription;

	private ParameterType parameterType;
	private UnitType unitType;
	private ComparisonType comparisonType;

	private Sports sport;

}
