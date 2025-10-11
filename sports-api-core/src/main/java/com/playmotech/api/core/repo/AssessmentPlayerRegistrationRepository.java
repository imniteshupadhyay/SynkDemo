package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration;
import com.playmotech.api.core.dao_postgres.AssessmentPlayerRegistration.PlayerStatus;

@Repository
public interface AssessmentPlayerRegistrationRepository extends JpaRepository<AssessmentPlayerRegistration, String>,
		JpaSpecificationExecutor<AssessmentPlayerRegistration> {

	List<AssessmentPlayerRegistration> findByRegistrationIdIn(List<String> registrationId);

	List<AssessmentPlayerRegistration> findByAssessment_IdAndPlayerStatus(String assessmentId,
			PlayerStatus playerStatus);

	List<AssessmentPlayerRegistration> findByAssessment_IdAndPlayerStatusIn(String id,
			List<PlayerStatus> statusesToCheck);

	List<AssessmentPlayerRegistration> findByAssessment_Id(String assessmentId);

	long countByAssessment_Id(String assessmentId);

}
