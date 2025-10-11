package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.dao_postgres.Assessment;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, String>, JpaSpecificationExecutor<Assessment> {

	List<Assessment> findByAssessmentStatusIn(List<AssessmentStatus> includedStatuses, Sort sort);

	List<Assessment> findByAssessmentStatusNotIn(List<AssessmentStatus> excludedStatuses, Sort sort);

	// For domain-linked academies
	List<Assessment> findByAcademyMappings_Academy_IdInAndAssessmentStatusNotIn(List<String> academyIds,
			List<AssessmentStatus> excludedStatuses, Sort sort);

	List<Assessment> findByAcademyMappings_Academy_IdInOrForAllAcademiesTrueAndAssessmentStatusNotIn(
			List<String> academyIds, List<AssessmentStatus> excludedStatuses, Sort sort);

	List<Assessment> findByAcademyMappings_Academy_IdInOrForAllAcademiesTrueAndAssessmentStatusIn(
			List<String> academyIds, List<AssessmentStatus> includedStatuses, Sort sort);

	List<Assessment> findByAssessmentStatus(AssessmentStatus closed);

}
