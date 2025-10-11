package com.playmotech.api.core.response.dao;

import java.util.List;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.constants.Sports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentDetails {

	private String id;

	private String assessmentTitle;
	private String assessmentDescription;
	private Boolean allAcademies;
	private Sports sport;
	private AgeCategory ageCategory;
	private AssessmentStatus assessmentStatus;
	private List<AssessmentAcademyMappingDao> academyMappings;
	private List<Long> academyIds;

}
