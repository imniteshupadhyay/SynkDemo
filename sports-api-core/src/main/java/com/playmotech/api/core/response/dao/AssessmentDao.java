package com.playmotech.api.core.response.dao;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentDao {

	private String id;

	private String assessmentTitle;
	private String assessmentDescription;
	private Boolean forAllAcademies;

	private Date startDate;
	private Date endDate;

	private String location;

	private Sports sport;
	private Gender gender;
	private AgeCategory ageCategory;
	private AssessmentStatus assessmentStatus;

	private Double registrationFee;

	private Timestamp createdOn;
	private Timestamp updatedOn;

	private List<AssessmentParameterDao> parameters;
	private List<AssessmentAcademyMappingDao> academyMappings;
	private List<String> academyIds;

}
