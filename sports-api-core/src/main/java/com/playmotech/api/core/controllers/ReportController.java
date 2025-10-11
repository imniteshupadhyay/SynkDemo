package com.playmotech.api.core.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.dto.ReportResponseDto;
import com.playmotech.api.core.dto.ReportSummaryDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IReportService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/reports")
@AllArgsConstructor
public class ReportController extends BaseController {

	private final IReportService reportService;

	@GetMapping(value = "/academies/{academyId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getEnrolledTraineesMonthly(
			@PathVariable("academyId") String academyId, @RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam(value = "timezone", required = false, defaultValue = "Asia/Kolkata") String timezone) {
		try {
			List<ReportResponseDto> responseDtos = reportService.getEnrolledTraineesMonthly(academyId, startDate,
					endDate, timezone);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/payments/received", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getTotalPaymentsReceived(
			@PathVariable("academyId") String academyId, @RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam(value = "timezone", required = false, defaultValue = "Asia/Kolkata") String timezone,
			@RequestParam(value = "courseIds", required = false) List<String> courseIds,
			@RequestParam(value = "sport", required = false) Sports sport) {
		try {
			List<ReportResponseDto> responseDtos = reportService.getTotalPaymentReceived(academyId, startDate, endDate,
					timezone, courseIds, sport);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/payments/pending/amount", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getPaymentPendingAmount(
			@PathVariable("academyId") String academyId,
			@RequestParam(value = "courseIds", required = false) List<String> courseIds,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			// TODO- New Payment Changes
			// List<ReportResponseDto> responseDtos =
			// reportService.getPendingDuesAmount(academyId, courseIds);

			List<ReportResponseDto> responseDtos = reportService.getNewPendingDuesAmount(academyId, courseIds,
					paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/paymentss/pending/amount", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getPaymentsPendingAmount(
			@PathVariable("academyId") String academyId,
			@RequestParam(value = "courseIds", required = false) List<String> courseIds,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<ReportResponseDto> responseDtos = reportService.getNewPendingDuesAmount(academyId, courseIds,
					paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/payments/pending/count", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getPaymentPendingCount(
			@PathVariable("academyId") String academyId,
			@RequestParam(value = "courseIds", required = false) List<String> courseIds,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			// TODO- New Payment Changes
			// List<ReportResponseDto> responseDtos =
			// reportService.getPendingDuesCount(academyId, courseIds);

			List<ReportResponseDto> responseDtos = reportService.getNewPendingDuesCount(academyId, courseIds,
					paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/paymentss/pending/count", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getPaymentsPendingCount(
			@PathVariable("academyId") String academyId,
			@RequestParam(value = "courseIds", required = false) List<String> courseIds,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<ReportResponseDto> responseDtos = reportService.getNewPendingDuesCount(academyId, courseIds,
					paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/trainees/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportResponseDto>>> getEnrolledTraineesAttendanceDaily(
			@PathVariable("academyId") String academyId, @RequestParam("startDate") String startDate,
			@RequestParam("endDate") String endDate,
			@RequestParam(value = "timezone", required = false, defaultValue = "Asia/Kolkata") String timezone,
			@RequestParam(value = "courseIds", required = false) List<String> courseId,
			@RequestParam(value = "sport", required = false) Sports sport) {
		try {
			List<ReportResponseDto> responseDtos = reportService.getTraineesAttendanceDaily(academyId, startDate,
					endDate, timezone, courseId, sport);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportResponseDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportResponseDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/summaries", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportSummaryDto>>> summaries(@PathVariable("academyId") String academyId,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<ReportSummaryDto> responseDtos = reportService.getNewReportSummaries(academyId, paymentCategory);

			// TODO- New Payment Changes
			// List<ReportSummaryDto> responseDtos =
			// reportService.getReportSummaries(academyId);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportSummaryDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportSummaryDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping(value = "/academies/{academyId}/summariess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<ReportSummaryDto>>> newSummaries(@PathVariable("academyId") String academyId,
			@RequestParam(value = "courseIds", required = false) List<String> courseIds,
			@RequestParam(value = "paymentCategory", defaultValue = "REGISTRATION_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			List<ReportSummaryDto> responseDtos = reportService.getNewReportSummaries(academyId, paymentCategory);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<ReportSummaryDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(responseDtos).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<ReportSummaryDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}
}
