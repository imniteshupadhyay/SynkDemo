package com.playmotech.api.core.repo;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.Status;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;

import jakarta.transaction.Transactional;

@Repository
public interface TraineeCourseEnrollmentRepo extends JpaRepository<TraineeCourseEnrollment, String> {
	List<TraineeCourseEnrollment> findByCourse_Id(String courseId);

	List<TraineeCourseEnrollment> findByCourse_IdAndTraineeUserProfile_Id(String courseId, String traineeUserId);

	List<TraineeCourseEnrollment> findByCourse_IdAndTraineeUserProfile_IdIn(String courseId,
			List<String> traineeUserIds);

	List<TraineeCourseEnrollment> findByTraineeUserProfile_IdAndAcademy_Id(String traineeUserId, String academyId);

	List<TraineeCourseEnrollment> findByAcademy_IdInAndTraineeUserProfile_Id(List<String> academyIds,
			String traineeUserId);

	List<TraineeCourseEnrollment> findByCourse_IdInAndTraineeUserProfile_Id(List<String> courseIds,
			String traineeUserId);

	List<TraineeCourseEnrollment> findByCourse_IdAndAcademy_Id(String courseId, String academyId);

	List<TraineeCourseEnrollment> findByAcademy_Id(String academyId);

	List<TraineeCourseEnrollment> findByAcademy_IdAndCreatedOnLessThanAndCreatedOnGreaterThanEqual(String academyId,
			Timestamp createdOn, Timestamp createdOn2);

	List<TraineeCourseEnrollment> findByTraineeUserProfile_Id(String traineeUserId);

	List<TraineeCourseEnrollment> findByAcademy_IdAndIdIn(String academyId, List<String> ids);

	List<TraineeCourseEnrollment> findByIdIn(List<String> ids);

	List<TraineeCourseEnrollment> findByCourse_IdIn(List<String> courseIds);

	@Modifying
	@Transactional
	@Query("UPDATE TraineeCourseEnrollment t SET t.status = 'INACTIVE' WHERE t.traineeUserProfile.id = :userId and t.academy.id = :academyId")
	void markTraineeEnrollmentInactiveByTraineeUserIdAndAcademyId(@Param("academyId") String academyId,
			@Param("userId") String userId);

	boolean existsByCourseIdAndTraineeUserProfileIdAndStatus(String programId, String id, Status active);

	/**
	 * Find enrollments that have dues to process More efficient than loading all
	 * enrollments and filtering in memory
	 */
	@Query(value = "SELECT * FROM trainee_course_enrollments "
			+ "WHERE dues_on IS NOT NULL AND (final_due_amount IS NOT NULL AND final_due_amount > 0)", nativeQuery = true)
	List<TraineeCourseEnrollment> findEnrollmentsWithDues();

	/**
	 * Find enrollments that have dues to process, paged to avoid loading all into
	 * memory at once.
	 * 
	 * @param page     Page index (0-based)
	 * @param pageSize Number of records per page
	 */
	@Query(value = "SELECT * FROM trainee_course_enrollments " + "WHERE dues_on IS NOT NULL "
			+ "AND dues_on <= CURRENT_DATE " + "AND final_due_amount IS NOT NULL " + "AND final_due_amount > 0 "
			+ "ORDER BY course_id, id " + "LIMIT :pageSize OFFSET (:page * :pageSize)", nativeQuery = true)
	List<TraineeCourseEnrollment> findEnrollmentsWithDuesPaged(int page, int pageSize);

	// 2. Filter by academyId and programId
	@Query(value = "SELECT * FROM trainee_course_enrollments " + "WHERE dues_on IS NOT NULL "
			+ "AND dues_on <= CURRENT_DATE " + "AND final_due_amount IS NOT NULL " + "AND final_due_amount > 0 "
			+ "AND academy_id = :academyId " + "AND course_id = :programId " + "ORDER BY course_id, id "
			+ "LIMIT :pageSize OFFSET (:page * :pageSize)", nativeQuery = true)
	List<TraineeCourseEnrollment> findEnrollmentsByAcademyAndProgramWithDuesPaged(String academyId, String programId,
			int page, int pageSize);

	// 3. Filter only by academyId
	@Query(value = "SELECT * FROM trainee_course_enrollments " + "WHERE dues_on IS NOT NULL "
			+ "AND dues_on <= CURRENT_DATE " + "AND final_due_amount IS NOT NULL " + "AND final_due_amount > 0 "
			+ "AND academy_id = :academyId " + "ORDER BY course_id, id "
			+ "LIMIT :pageSize OFFSET (:page * :pageSize)", nativeQuery = true)
	List<TraineeCourseEnrollment> findEnrollmentsByAcademyWithDuesPaged(String academyId, int page, int pageSize);

}
