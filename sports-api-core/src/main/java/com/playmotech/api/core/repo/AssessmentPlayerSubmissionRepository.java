package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerSubmission.SubmissionStatus;

@Repository
public interface AssessmentPlayerSubmissionRepository extends JpaRepository<AssessmentPlayerSubmission, String>,
		JpaSpecificationExecutor<AssessmentPlayerSubmission> {

	Optional<AssessmentPlayerSubmission> findByRegistration_RegistrationId(String registrationId);

	List<AssessmentPlayerSubmission> findByAssessment_Id(String assessmentId);

	List<AssessmentPlayerSubmission> findByAssessment_IdInAndSubmissionStatus(List<String> closedAssessmentIds,
			SubmissionStatus submitted);

}
