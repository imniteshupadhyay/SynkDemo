package com.playmotech.api.core.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.AssessmentAcademyMapping;

@Repository
public interface AssessmentAcademyMappingRepository extends JpaRepository<AssessmentAcademyMapping, String> {

//	List<AssessmentAcademyMapping> findByAcademy_IdInAndAssessment_SportAndAssessment_AgeCategoryAndAssessment_Gender(
//			List<String> academyIds, Sports sport, AgeCategory ageCategory, Gender gender);
}
