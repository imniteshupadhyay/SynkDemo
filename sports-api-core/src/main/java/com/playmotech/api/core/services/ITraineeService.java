package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PerformanceReportStatus;
import com.playmotech.api.core.dao_postgres.TraineeAcademyMapping;
import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.EditTraineePerformanceReportRequestDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.SubmitTraineePerformanceReportRequestDto;
import com.playmotech.api.core.dto.ToggleTraineeStatusDto;
import com.playmotech.api.core.dto.TraineeDetailsDto;
import com.playmotech.api.core.dto.TraineePaymentDetailsDto;
import com.playmotech.api.core.dto.TraineePerformanceReportDto;
import com.playmotech.api.core.dto.TraineePerformanceReportPdfDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface ITraineeService {
	void addTraineesToAcademy(List<String> traineeUserIds, String academyId) throws ResourceException;

	List<TraineeAcademyMapping> getEnrollmentsByAcademyId(String academyId, Long startEpoch, Long endEpoch)
			throws ResourceException;

	List<TraineeAcademyMapping> getTraineeAcademyMappingByTrainee(String traineeUserId) throws ResourceException;

	TraineeDetailsDto getTraineeById(String academyId, String traineeUserId) throws ResourceException;

	List<TraineeDetailsDto> getTraineesByAcademyIdAndNameAndPhoneNumber(String academyId, String name, String phone,
			String searchTxt) throws ResourceException;

	void removeTraineesFromAcademy(List<String> traineeUserIds, String academyId) throws ResourceException;

	void toggleTraineeStatusInAcademy(String academyId, ToggleTraineeStatusDto toggleTraineeStatusDto)
			throws ResourceException;

	TraineePerformanceReportDto submitTraineePerformanceReport(String coachUserId, String traineeUserId,
			SubmitTraineePerformanceReportRequestDto traineePerformanceDto, List<FileObjectDto> fileObjectDtos)
			throws ResourceException;

	void editTraineePerformanceReport(String id, String coachUserId, String traineeUserId,
			EditTraineePerformanceReportRequestDto traineePerformanceDto, List<FileObjectDto> fileObjectDtos)
			throws ResourceException;

	List<TraineePerformanceReportDto> getTraineePerformances(String traineeUserId, String courseId)
			throws ResourceException;

	List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserId(String coachUserId,
			PerformanceReportStatus performanceReportStatus) throws ResourceException;

	List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserIdAndAcademyId(String academyId,
			String coachUserId, PerformanceReportStatus performanceReportStatus, String courseId)
			throws ResourceException;

	List<TraineePerformanceReportDto> getTraineePerformancesByCoachUserIdAndTraineeIdAndAcademyId(String academyId,
			String coachUserId, String traineeId, PerformanceReportStatus performanceReportStatus, String courseId)
			throws ResourceException;

	List<AcademyDto> getMyAcademies(String traineeUserId) throws ResourceException;

	void submitPerformanceReport(String userId, String performanceReportId) throws ResourceException;

	void deletePerformanceReport(String userId, String performanceReportId) throws ResourceException;

	TraineePerformanceReportPdfDto getPerformanceReportPdf(String userId, String performanceReportId)
			throws ResourceException;

	List<UserProfileMinDto> getRegisteredUsers(String academyId) throws ResourceException;

	byte[] generatePerformanceReportPdf(String performanceReportId) throws ResourceException;

	List<TraineePaymentDetailsDto> getTraineePayments(String academyId, String traineeUserId, PaymentCategory category)
			throws ResourceException;

	ServiceResponse getAllTrainess(GenericFilter filter, String domainUrl);

	byte[] generateCoachPerformanceReportPdf(String performanceReportId) throws ResourceException;

	List<TraineePaymentDetailsDto> getNewTraineePayments(String academyId, String traineeUserId,
			PaymentCategory category) throws ResourceException;

}
