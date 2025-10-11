package com.playmotech.api.core.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.ITraineeService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/performance/reports")
@AllArgsConstructor
public class PerformanceReportController extends BaseController {

	private final ITraineeService traineeService;

	@GetMapping(value = "/pdf/{performanceReportId}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity downloadPdf(@PathVariable("performanceReportId") String performanceReportId) {
		try {
			byte[] pdf = traineeService.generatePerformanceReportPdf(performanceReportId);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + performanceReportId + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}
	
	@GetMapping(value = "/program-feedback/pdf/{performanceReportId}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity downloadProgramFeedbackPdf(@PathVariable("performanceReportId") String performanceReportId) {
		try {
			byte[] pdf = traineeService.generateCoachPerformanceReportPdf(performanceReportId);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + performanceReportId + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}
	
	@GetMapping(value = "/coach-feedback/pdf/{performanceReportId}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity downloadCoachFeedbackPdf(@PathVariable("performanceReportId") String performanceReportId) {
		try {
			byte[] pdf = traineeService.generateCoachPerformanceReportPdf(performanceReportId);
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + performanceReportId + ".pdf")
					.contentType(MediaType.APPLICATION_PDF).body(pdf);
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(
					Response.builder().status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}
	
}