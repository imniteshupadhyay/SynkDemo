package com.playmotech.api.core.dto;

import java.sql.Date;
import java.util.List;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;

import lombok.Data;

@Data
public class AssessmentDto {

	private String id;
	private String assessmentTitle;
	private String assessmentDescription;
	private Boolean forAllAcademies;
	private Date startDate;
	private Date endDate;
	private String location;
	private Double registrationFee;
	private AssessmentStatus assessmentStatus;
	private Sports sport;
	private List<AssessmentParameterDto> parameters;
	private List<String> academyIds;

}
