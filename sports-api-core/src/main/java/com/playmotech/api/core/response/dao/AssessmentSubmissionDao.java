package com.playmotech.api.core.response.dao;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSubmissionDao {

	private String submissionId;

	private String registrationId;
	private String registrationNumber;

	private String assessmentId;
	private String playerId;
	private String playerName;
	private String playerNumber;

	private Gender gender;
	private String academyId;
	private String academyName;
	private Timestamp submittedOn;
	private SubmissionStatus submissionStatus;
	private Timestamp createdOn;
	private String createdBy;
	private Timestamp updatedOn;
	private String updatedBy;

	private List<AssessmentSubmissionParameterDao> parameters;
}
