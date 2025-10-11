package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.ReportResponseDto;
import com.playmotech.api.core.dto.ReportSummaryDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IReportService {
	List<ReportResponseDto> getEnrolledTraineesMonthly(String academyId, String startDate, String endDate,
			String timezone) throws ResourceException;

	List<ReportResponseDto> getTraineesAttendanceDaily(String academyId, String startDate, String endDate,
			String timezone, List<String> courseId, Sports sport) throws ResourceException;

	List<ReportResponseDto> getTotalPaymentReceived(String academyId, String startDate, String endDate, String timezone,
			List<String> courseIds, Sports sport) throws ResourceException;

	List<ReportResponseDto> getPendingDuesCount(String academyId, List<String> courseIds) throws ResourceException;

	List<ReportResponseDto> getPendingDuesAmount(String academyId, List<String> courseIds) throws ResourceException;

	List<ReportSummaryDto> getReportSummaries(String academyId) throws ResourceException;

	List<ReportResponseDto> getNewPendingDuesAmount(String academyId, List<String> courseIds, PaymentCategory category)
			throws ResourceException;

	List<ReportResponseDto> getNewPendingDuesCount(String academyId, List<String> courseIds,
			PaymentCategory paymentCategory) throws ResourceException;

	List<ReportSummaryDto> getNewReportSummaries(String academyId, PaymentCategory paymentCategory)
			throws ResourceException;
}
