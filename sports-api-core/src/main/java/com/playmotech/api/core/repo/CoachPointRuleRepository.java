package com.playmotech.api.core.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CoachPointRule;

@Repository
public interface CoachPointRuleRepository extends JpaRepository<CoachPointRule, Long> {

    // Academy-specific rules
    CoachPointRule findByActionTypeAndAcademyIdAndIsAcademySpecificTrueAndActiveTrue(String actionType, String academyId);

    List<CoachPointRule> findByAcademyIdAndIsAcademySpecificTrueAndActiveTrue(String academyId);

    // Organization-specific rules (not academy-specific)
    CoachPointRule findByActionTypeAndOrganisationIdAndIsAcademySpecificFalseAndActiveTrue(String actionType, String organisationId);

    List<CoachPointRule> findByOrganisationIdAndIsAcademySpecificFalseAndActiveTrue(String organisationId);

    // Legacy methods - maintained for backward compatibility
    List<CoachPointRule> findByOrganisationIdAndActiveTrue(String organisationId);

    CoachPointRule findByActionTypeAndOrganisationIdAndActiveTrue(String actionType, String organisationId);

    List<CoachPointRule> findByActiveTrue();

    List<CoachPointRule> findByAcademyIdAndOrganisationIdAndActiveTrue(String academyId, String organisationId);
}
