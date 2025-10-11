package com.playmotech.api.core.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.dao_postgres.CourseAttendance;

@Repository
public interface CourseAttendanceRepo extends CrudRepository<CourseAttendance, String> {
	Optional<CourseAttendance> findByCourse_IdAndNormalizedDate(String courseId, Long normalizedDate);

	List<CourseAttendance> findByCourse_IdAndNormalizedDateIn(String courseId, List<Long> normalizedDate);

	List<CourseAttendance> findByAcademy_IdAndNormalizedDateGreaterThanEqualAndNormalizedDateLessThanEqual(
			String courseId, Long startDate, Long endDate);
}
