package com.playmotech.api.core.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CoachPointsBalance;
import com.playmotech.api.core.dto.CoachLeaderboardProjection;

@Repository
public interface CoachPointsBalanceRepository
          extends JpaRepository<CoachPointsBalance, String>, JpaSpecificationExecutor<CoachPointsBalance> {

     // Find the organization-wide balance for a coach
     Optional<CoachPointsBalance> findByCoach_IdAndIsAcademySpecificFalse(String coachId);

     // Find academy-specific balance for a coach
     Optional<CoachPointsBalance> findByCoach_IdAndAcademy_Id(String coachId, String academyId);

     // Find organisation-specific balance for a coach
     Optional<CoachPointsBalance> findByCoach_IdAndOrganisation_Id(String coachId, String organisationId);

     @Query("SELECT c FROM CoachPointsBalance c WHERE c.coach.id = :coachId AND (c.academy.id = :academyId OR c.organisation.id = :organisationId)")
     Optional<CoachPointsBalance> findCoachByIdInAcademyOrOrganisation(String coachId, String academyId,
               String organisationId);

     // Find all balances for a coach (both org-wide and academy-specific)
     List<CoachPointsBalance> findByCoach_Id(String coachId);

     // Find organization leaderboard using direct organization_id field
     @Query("SELECT b FROM CoachPointsBalance b WHERE b.organisation.id = :organisationId AND b.isAcademySpecific = false ORDER BY b.totalEarned DESC")
     Page<CoachPointsBalance> findLeaderboardByOrganisationId(@Param("organisationId") String organisationId,
               Pageable pageable);

     // Find organization leaderboard with time period filter
     @Query("SELECT b FROM CoachPointsBalance b JOIN b.coach c " +
               "WHERE b.organisation.id = :organisationId " +
               // "AND b.isAcademySpecific = false " +
               "AND b.lastUpdatedAt >= :startDate")
     Page<CoachPointsBalance> findLeaderboardByOrganisationIdWithTimeFilter(
               @Param("organisationId") String organisationId,
               @Param("startDate") LocalDateTime startDate,
               Pageable pageable);

     // Find organization leaderboard with search and time period filter
     @Query("SELECT b FROM CoachPointsBalance b JOIN b.coach c " +
               "WHERE b.organisation.id = :organisationId " +
               // "AND b.isAcademySpecific = false " +
               "AND b.lastUpdatedAt >= :startDate " +
               "AND (LOWER(c.displayName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
               "LOWER(c.username) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) ")
     Page<CoachPointsBalance> findLeaderboardByOrganisationIdWithSearchAndTimeFilter(
               @Param("organisationId") String organisationId,
               @Param("searchQuery") String searchQuery,
               @Param("startDate") LocalDateTime startDate,
               Pageable pageable);

     // Find academy-specific leaderboard using academy_id field
     @Query("SELECT b FROM CoachPointsBalance b WHERE b.academy.id = :academyId AND b.isAcademySpecific = true ORDER BY b.totalEarned DESC")
     Page<CoachPointsBalance> findLeaderboardByAcademyId(@Param("academyId") String academyId,
               Pageable pageable);

     // Find academy leaderboard with time period filter
     @Query("SELECT b FROM CoachPointsBalance b JOIN b.coach c " +
               "WHERE b.academy.id = :academyId " +
               // "AND b.isAcademySpecific = true " +
               "AND b.lastUpdatedAt >= :startDate")
     Page<CoachPointsBalance> findLeaderboardByAcademyIdWithTimeFilter(
               @Param("academyId") String academyId,
               @Param("startDate") LocalDateTime startDate,
               Pageable pageable);

     // Find academy leaderboard with search and time period filter
     @Query("SELECT b FROM CoachPointsBalance b JOIN b.coach c " +
               "WHERE b.academy.id = :academyId " +
               // "AND b.isAcademySpecific = true " +
               "AND b.lastUpdatedAt >= :startDate " +
               "AND (LOWER(c.displayName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
               "LOWER(c.username) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) ")
     Page<CoachPointsBalance> findLeaderboardByAcademyIdWithSearchAndTimeFilter(
               @Param("academyId") String academyId,
               @Param("searchQuery") String searchQuery,
               @Param("startDate") LocalDateTime startDate,
               Pageable pageable);

     @Query(value = """
               SELECT CASE
                   WHEN COALESCE((
                       SELECT SUM(total_earned)
                       FROM coach_points_balance
                       WHERE coach_id = :coachId
                       AND organisation_id = :organisationId
                   ), 0) = 0 THEN NULL
                   ELSE (
                       SELECT COALESCE(COUNT(DISTINCT coach_id), 0) + 1
                       FROM (
                           SELECT coach_id, SUM(total_earned) as coach_total
                           FROM coach_points_balance
                           WHERE organisation_id = :organisationId
                           GROUP BY coach_id
                       ) coach_totals
                       WHERE coach_total > (
                           SELECT SUM(total_earned)
                           FROM coach_points_balance
                           WHERE coach_id = :coachId
                           AND organisation_id = :organisationId
                       )
                   )
               END
               """, nativeQuery = true)
     Integer findCoachRankInOrganisation(@Param("coachId") String coachId,
               @Param("organisationId") String organisationId);

     @Query(value = """
               SELECT CASE
                   WHEN COALESCE((
                       SELECT total_earned
                       FROM coach_points_balance
                       WHERE coach_id = :coachId
                       AND academy_id = :academyId
                   ), 0) = 0 THEN NULL
                   ELSE (
                       SELECT COALESCE(COUNT(*), 0) + 1
                       FROM coach_points_balance
                       WHERE academy_id = :academyId
                       AND total_earned > (
                           SELECT total_earned
                           FROM coach_points_balance
                           WHERE coach_id = :coachId
                           AND academy_id = :academyId
                       )
                   )
               END
               """, nativeQuery = true)
     Integer findCoachRankInAcademy(@Param("coachId") String coachId,
               @Param("academyId") String academyId);

     /**
      * Batch fetch ranks for multiple coaches in an academy
      *
      * @param coachIds  List of coach IDs
      * @param academyId Academy ID
      * @return List of maps containing coachId and rank
      */
     @Query("SELECT b.coach.id as coachId, (COUNT(b2) + 1) as rank FROM CoachPointsBalance b, CoachPointsBalance b2 "
               +
               "WHERE b.academy.id = :academyId AND b2.academy.id = :academyId " +
               "AND b.coach.id IN :coachIds " +
               "AND b2.totalEarned > b.totalEarned " +
               "GROUP BY b.coach.id")
     List<Map<String, Object>> findBatchCoachRanksInAcademy(@Param("coachIds") List<String> coachIds,
               @Param("academyId") String academyId);

     /**
      * Batch fetch ranks for multiple coaches in an organization
      *
      * @param coachIds       List of coach IDs
      * @param organisationId Organization ID
      * @return List of maps containing coachId and rank
      */
     @Query("SELECT b.coach.id as coachId, (COUNT(b2) + 1) as rank FROM CoachPointsBalance b, CoachPointsBalance b2 "
               +
               "WHERE b.organisation.id = :organisationId AND b2.organisation.id = :organisationId " +
               "AND b.coach.id IN :coachIds " +
               "AND b2.totalEarned > b.totalEarned " +
               "GROUP BY b.coach.id")
     List<Map<String, Object>> findBatchCoachRanksInOrganisation(@Param("coachIds") List<String> coachIds,
               @Param("organisationId") String organisationId);

     // Global leaderboard with aggregation by userId
     @Query("""
               SELECT b.coach.id as coachId,
                      b.coach as coach,
                      SUM(b.totalEarned) as totalPoints,
                      MAX(b.lastUpdatedAt) as lastUpdatedAt
               FROM CoachPointsBalance b
               WHERE b.lastUpdatedAt >= :startDate
               AND b.totalEarned > 0
               AND (:searchQuery IS NULL OR :searchQuery = ''
                    OR LOWER(b.coach.displayName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                    OR LOWER(b.coach.username) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
               GROUP BY b.coach.id, b.coach
               ORDER BY SUM(b.totalEarned) DESC
               """)
     Page<CoachLeaderboardProjection> findGlobalLeaderboardAggregated(
               @Param("startDate") LocalDateTime startDate,
               @Param("searchQuery") String searchQuery,
               Pageable pageable);

     // Organisation leaderboard with aggregation by userId
     @Query("""
               SELECT b.coach.id as coachId,
                      b.coach as coach,
                      SUM(b.totalEarned) as totalPoints,
                      MAX(b.lastUpdatedAt) as lastUpdatedAt
               FROM CoachPointsBalance b
               WHERE b.organisation.id = :organisationId
               AND b.lastUpdatedAt >= :startDate
               AND b.totalEarned > 0
               AND (:searchQuery IS NULL OR :searchQuery = ''
                    OR LOWER(b.coach.displayName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                    OR LOWER(b.coach.username) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
               GROUP BY b.coach.id, b.coach
               ORDER BY SUM(b.totalEarned) DESC
               """)
     Page<CoachLeaderboardProjection> findOrganisationLeaderboardAggregated(
               @Param("organisationId") String organisationId,
               @Param("startDate") LocalDateTime startDate,
               @Param("searchQuery") String searchQuery,
               Pageable pageable);

     // Academy leaderboard with aggregation by userId
     @Query("""
               SELECT b.coach.id as coachId,
                      b.coach as coach,
                      SUM(b.totalEarned) as totalPoints,
                      MAX(b.lastUpdatedAt) as lastUpdatedAt
               FROM CoachPointsBalance b
               WHERE b.academy.id = :academyId
               AND b.lastUpdatedAt >= :startDate
               AND b.totalEarned > 0
               AND (:searchQuery IS NULL OR :searchQuery = ''
                    OR LOWER(b.coach.displayName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                    OR LOWER(b.coach.username) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
               GROUP BY b.coach.id, b.coach
               ORDER BY SUM(b.totalEarned) DESC
               """)
     Page<CoachLeaderboardProjection> findAcademyLeaderboardAggregated(
               @Param("academyId") String academyId,
               @Param("startDate") LocalDateTime startDate,
               @Param("searchQuery") String searchQuery,
               Pageable pageable);
}
