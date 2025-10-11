package com.playmotech.api.core.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CourseCoachMapping;

@Repository
public interface CourseCoachMappingRepo extends JpaRepository<CourseCoachMapping, Long> {

	boolean existsByCourseIdAndCoachUserProfileId(String id, String coachId);

	Optional<CourseCoachMapping> findByCoachUserProfile_IdAndCourse_Id(String coachId, String programId);

}
