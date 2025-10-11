package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao_postgres.Course;

@Repository
public interface CourseRepo extends CrudRepository<Course, String> {

	List<Course> findByAcademy_Id(String academyId);

	List<Course> findByCoachUserProfile_Id(String coachUserId);

	List<Course> findByCoachUserProfile_IdAndAcademy_Id(String coachUserId, String academyId);

	Optional<Course> findByAcademy_IdAndId(String academyId, String id);

	List<Course> findByAcademy_IdAndIdIn(String academyId, List<String> id);

	List<Course> findByAcademy_IdInAndIdIn(List<String> academyId, List<String> id);

	List<Course> findByIdIn(List<String> id);

	List<Course> findByVisibility(Visibility visibility);

	@Query("SELECT c FROM Course c JOIN c.courseCoachMappings ccm WHERE ccm.coachUserProfile.id = :coachId")
	List<Course> findCoursesByCoachId(@Param("coachId") String coachId);

	@Query("SELECT c FROM Course c JOIN c.courseCoachMappings ccm WHERE ccm.coachUserProfile.id = :coachId AND c.academy.id = :academyId")
	List<Course> findCoursesByCoachIdAndAcademyId(@Param("coachId") String coachId,
			@Param("academyId") String academyId);

	@Query(value = "SELECT * FROM get_coach_details( " + ":userId, " + ":academyIds, " + ":courseIds, " + ":sports, "
			+ ":userRole, " + ":p_domain_url) ", nativeQuery = true)
	List<Object[]> getCoachDetails(@Param("userId") String userId, @Param("academyIds") String academyIds,
			@Param("courseIds") String courseIds, @Param("sports") String sports, @Param("userRole") String userRole,
			@Param("p_domain_url") String pDomainUrl);

	Optional<Course> findByIdAndInactiveFalse(String id);

	List<Course> findByAcademyIdAndInactiveFalse(String academyId);

	Optional<Course> findByTitleAndAcademyId(@Param("title") String title, @Param("academyId") String academyId);

	boolean existsByTitleAndAcademyId(String title, String academyId);

}
