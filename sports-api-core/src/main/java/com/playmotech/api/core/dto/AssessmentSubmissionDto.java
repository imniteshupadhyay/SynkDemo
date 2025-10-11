package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;

import lombok.Data;

@Data
public class AssessmentSubmissionDto {

	private String submissionId;
	private String assessmentId;
	private String registrationId;
	private String academyId;
	private String playerId;
	private Gender gender;
	private SubmissionStatus submissionStatus;
	private List<AssessmentSubmissionParameterDto> submissionParameters;

}
