package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CoachActivityStreak;

/**
 * Repository for coach activity streak tracking
 */
@Repository
public interface CoachActivityStreakRepository extends JpaRepository<CoachActivityStreak, String> {

    List<CoachActivityStreak> findByCoachIdIn(List<String> coachIds);

    /**
     * Find a coach's organization-wide streak (not academy-specific)
     */
    Optional<CoachActivityStreak> findByCoachIdAndIsAcademySpecificFalse(String coachId);

    /**
     * Find a coach's streak for a specific academy
     */
    Optional<CoachActivityStreak> findByCoachIdAndAcademyId(String coachId, String academyId);

    /**
     * Find all streaks for a coach (both organization and academy-specific)
     */
    List<CoachActivityStreak> findByCoachId(String coachId);

    /**
     * Find all streaks in an academy
     */
    List<CoachActivityStreak> findByAcademyIdAndIsAcademySpecificTrue(String academyId);

    /**
     * Find all streaks in an organization (not academy-specific)
     */
    List<CoachActivityStreak> findByOrganisationIdAndIsAcademySpecificFalse(String organisationId);
}
