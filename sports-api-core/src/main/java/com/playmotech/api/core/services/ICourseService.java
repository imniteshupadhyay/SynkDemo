package com.playmotech.api.core.services;

import java.util.List;

import org.modelmapper.internal.Pair;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.Visibility;
import com.playmotech.api.core.dao_postgres.Course;
import com.playmotech.api.core.dao_postgres.ScheduleFile;
import com.playmotech.api.core.dao_postgres.TraineeCourseEnrollment;
import com.playmotech.api.core.dto.CourseDto;
import com.playmotech.api.core.dto.CreateCourseDto;
import com.playmotech.api.core.dto.EnrollTraineeInCourseDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.ScheduleFileDto;
import com.playmotech.api.core.dto.TraineeCourseEnrollmentDto;
import com.playmotech.api.core.dto.UpdateCourseDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface ICourseService {

	CourseDto createCourse(String academyId, String userId, CreateCourseDto createCourseDto, boolean isBulk)
			throws ResourceException;

	CourseDto updateCourse(String academyId, String courseId, UpdateCourseDto updateCourseDto) throws ResourceException;

	String updateCoursePicture(String academyId, String courseId, String userId, FileObjectDto fileObjectDto)
			throws ResourceException;

	CourseDto getCourse(String academyId, String courseId) throws ResourceException;

	List<CourseDto> getCourses(String academyId, List<String> courseId, Boolean isActive, Visibility visibility)
			throws ResourceException;

	List<CourseDto> getCoursesByAcademyId(String userId, String academyId, String traineeId, Sports sport,
			Boolean isActive, Visibility visibility, Boolean filterTournaments, Boolean excludeEnrolledCourses,
			String searchTxt, String orgId) throws ResourceException;

	List<CourseDto> getCoursesByCoachUserId(String coachUserId, String academyId, Boolean isActive,
			Visibility visibility, Boolean filterTournaments, String searchTxt) throws ResourceException;

	List<TraineeCourseEnrollmentDto> getEnrolledTrainees(String academyId, String courseId) throws ResourceException;

	List<CourseDto> getEnrolledCourses(String academyId, String traineeUserId, Boolean isActive, Visibility visibility)
			throws ResourceException;

	List<TraineeCourseEnrollment> getEnrollmentsByTraineeUserId(String academyId, String traineeUserId)
			throws ResourceException;

	List<TraineeCourseEnrollment> getEnrollmentsByCourseId(String academyId, String traineeUserId)
			throws ResourceException;

	List<TraineeCourseEnrollment> getEnrollmentsByAcademyId(String academyId) throws ResourceException;

	List<TraineeCourseEnrollment> getEnrollmentsByAcademyId(String academyId, List<String> ids)
			throws ResourceException;

	// void enrollTraineesInCourse(String academyId, String courseId, List<String>
	// traineeUserIds) throws ResourceException;
	void enrollTraineesInCourseWithPaymentOptions(String academyId, String courseId,
			EnrollTraineeInCourseDto traineeUsers) throws ResourceException;

	void updateTraineeEnrollment(String academyId, String courseId, String enrollmentId,
			EnrollTraineeInCourseDto updateTraineeEnrollmentDto) throws ResourceException;

	void unEnrollTraineeFromAllCourseInAcademy(String academyId, String traineeUserId);

	void enrollmentExists(String enrollmentId) throws ResourceException;

	Pair<TraineeCourseEnrollmentDto, CourseDto> getByEnrollmentId(String enrollmentId) throws ResourceException;

	List<Pair<TraineeCourseEnrollmentDto, CourseDto>> getByEnrollmentIds(List<String> enrollmentId)
			throws ResourceException;

	List<TraineeCourseEnrollmentDto> getByEnrollmentIdsByEID(List<String> enrollmentId) throws ResourceException;

	List<CourseDto> adaptCourseDtos(List<Course> courses);

	List<UserProfileMinDto> getCoachesByCourse(String courseId);

	public ScheduleFileDto toScheduleFileDto(ScheduleFile scheduleFile);

	/**
	 * Interface which helps update course with file.
	 * 
	 * @param academyId
	 * @param courseId
	 * @param updateCourseDto
	 * @param file
	 * @return {@link CourseDto}
	 */
	CourseDto updateCourseWithFile(String userId, String academyId, String courseId, UpdateCourseDto updateCourseDto,
			MultipartFile file) throws ResourceException;

	/**
	 * Interface which helps create course with file.
	 * 
	 * @param academyId
	 * @param courseId
	 * @param {@link    CreateCourseDto}
	 * @param isBulk
	 * @param file
	 * @return {@link CourseDto}
	 */
	CourseDto createCourseWithFile(String academyId, String userId, CreateCourseDto createCourseDto, boolean isBulk,
			MultipartFile file) throws ResourceException;

}
