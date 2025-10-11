package com.playmotech.api.core.repo;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.playmotech.api.core.response.dao.AttendanceTrendDao;
import com.playmotech.api.core.views.AttendanceView;

public interface AttendanceViewRepository
		extends JpaRepository<AttendanceView, String>, JpaSpecificationExecutor<AttendanceView> {

	@Query(value = "SELECT * FROM get_attendance_summary(:startDate, :endDate, :userId, :academyIds, :sports, :courseIds, :ageCategories, :userRole, :domainUrl)", nativeQuery = true)
	List<Object[]> getAttendanceTrendRaw(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate,

			@Param("userId") String userId, @Param("academyIds") String academyIds, @Param("sports") String sports,
			@Param("courseIds") String courseIds, @Param("ageCategories") String ageCategories,
			@Param("userRole") String userRole, @Param("domainUrl") String domainUrl);

	default List<AttendanceTrendDao> getAttendanceTrend(Timestamp startDate, Timestamp endDate, String userId,
			String academyIds, String sports, String courseIds, String ageCategories, String userRole,
			String domainUrl) {
		List<Object[]> results = getAttendanceTrendRaw(startDate, endDate, userId, academyIds, sports, courseIds,
				ageCategories, userRole, domainUrl);
		return results.stream().map(row -> new AttendanceTrendDao(row[0] != null ? (String) row[0] : null, // period_label
				row[1] != null ? ((Number) row[1]).longValue() : null, // total_students
				row[2] != null ? ((Number) row[2]).longValue() : null, // avg_attendance
				row[3] != null ? ((Number) row[3]).longValue() : null, // total_sessions
				row[4] != null ? ((Number) row[4]).longValue() : null // previous_period_avg_attendance
		)).toList();
	}

	@Query(value = "SELECT * FROM get_player_added_count( " + ":startDate, " + ":endDate, " + ":userId, "
			+ ":academyIds, " + ":sports, " + ":courseIds, " + ":ageCategories," + ":userRole, "
			+ ":domainUrl ) ", nativeQuery = true)
	List<Object[]> findPlayerAddedDetails(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate,

			@Param("userId") String userId, @Param("academyIds") String academyIds,
			@Param("courseIds") String courseIds, // Pass // as
			@Param("sports") String sports, @Param("ageCategories") String ageCategories,
			@Param("userRole") String userRole, @Param("domainUrl") String domainUrl);

	@Query(value = "SELECT * FROM get_attendance_percentage( " + ":startDate, " + ":endDate, " + ":userId, "
			+ ":academyIds, "

			+ ":sports, " + ":courseIds, " + ":ageCategories," + ":userRole, " + ":domainUrl ) ", nativeQuery = true)
	List<Object[]> findAttendanceDetails(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate,
			@Param("userId") String userId, @Param("academyIds") String academyIds,
			@Param("courseIds") String courseIds, // Pass
													// as
													// comma-separated
													// string
			@Param("sports") String sports, @Param("ageCategories") String ageCategories,
			@Param("userRole") String userRole, @Param("domainUrl") String domainUrl);

	List<AttendanceView> findByEnrollId(String id);

	List<AttendanceView> findByPlayerId(String identifier);

	@Query(value = "SELECT * FROM attendance_view a WHERE :userId = ANY(a.coach_ids)", nativeQuery = true)
	List<AttendanceView> findByCoachId(@Param("userId") String coachId);

	List<AttendanceView> findByMarkedById(String coachId);

}