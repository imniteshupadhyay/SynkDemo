package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.GeoFenceCoachAttendance;
import com.playmotech.api.core.dao_postgres.UserProfile;

/**
 * Repository for Attendance entity
 */
@Repository
public interface GeoFenceCoachAttendanceRepository extends JpaRepository<GeoFenceCoachAttendance, Long> {

	/**
	 * Find active attendance for a coach in specified academy and program
	 */
	Optional<GeoFenceCoachAttendance> findByCoach_IdAndAcademy_IdAndProgram_IdAndIsActiveTrueAndDeletedFalse(
			String coachId, String academyId, String programId);

	/**
	 * Find any active attendance for a coach
	 */
	List<GeoFenceCoachAttendance> findByCoach_IdAndIsActiveTrueAndDeletedFalse(String coachId);

	/**
	 * Find all attendances for a coach in an academy
	 */
	List<GeoFenceCoachAttendance> findByCoach_IdAndAcademy_IdAndDeletedFalse(String coachId, String academyId);

	/**
	 * Find attendance by entity references
	 */
	Optional<GeoFenceCoachAttendance> findByCoachAndAcademyAndProgramAndIsActiveTrueAndDeletedFalse(UserProfile coach,
			Academy academy, Course program);

	Optional<GeoFenceCoachAttendance> findByCoach_IdAndProgram_IdAndIsActiveTrueAndDeletedFalse(String coachId,
			String programId);

	/**
	 * Find all active attendances for an academy
	 */
	List<GeoFenceCoachAttendance> findByAcademy_IdAndIsActiveTrueAndDeletedFalse(String academyId);

	List<GeoFenceCoachAttendance> findByIsActiveTrueAndDeletedFalse();
}
