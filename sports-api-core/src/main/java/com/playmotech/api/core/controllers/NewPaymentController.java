package com.playmotech.api.core.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.dto.DiscountRequest;
import com.playmotech.api.core.dto.InitPaymentDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UpdatePaymentDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.NewPaymentService;
import com.playmotech.api.core.services.impl.DuesUpdateScheduler;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/paymentss")
@AllArgsConstructor
public class NewPaymentController {

	private final NewPaymentService paymentService;

	@GetMapping(value = "/update-dues/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> generateDuesPreview(@PathVariable("enrollmentId") String enrollmentId,
			@RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		// Use provided date or default to today
		LocalDate todayDate = (date != null) ? date : LocalDate.now();

		ServiceResponse response = paymentService.updateDues(enrollmentId, todayDate);
		return ResponseEntity.status(response.getHttpStatus()).body(response);

	}

	@GetMapping(value = "dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> getDues(@RequestParam("userId") String userId,
			@RequestParam("courseId") String courseId) {

		ServiceResponse response = paymentService.getDues(userId, courseId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);

	}

	@PostMapping(value = "discount", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> addDiscount(@RequestBody DiscountRequest request) {
		ServiceResponse response = paymentService.addDiscount(request);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping(value = "ledgers", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> getLedgers(@RequestParam("enrollmentId") String enrollmentId) {
		ServiceResponse response = paymentService.getLedgers(enrollmentId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PutMapping("/mark-pending")
	public ResponseEntity<ServiceResponse> markPaymentsPendingOnly(@RequestParam String enrollmentId) {
		ServiceResponse response = paymentService.markPaymentsPendingOnly(enrollmentId);

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PutMapping("/update-success-only")
	public ResponseEntity<ServiceResponse> updatePaymentsToSuccessOnly(@RequestParam String enrollmentId,
			@RequestBody List<String> paymentIds) {
		ServiceResponse response = paymentService.updatePaymentsToSuccessOnly(paymentIds, enrollmentId);

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping("/generate-dues")
	public ResponseEntity<ServiceResponse> generateDuesForEnrollment(@RequestParam String enrollmentId) {
		ServiceResponse response = paymentService.generateDuesForEnrollment(enrollmentId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	private final DuesUpdateScheduler duesUpdateScheduler;

	@PostMapping("schedule-runner")
	public void generateDues() {
		duesUpdateScheduler.processDuesUpdates();
	}

	@PostMapping("updated")
	public void generateDuesForAcademyProgram(@RequestParam String academyId, @RequestParam String programId) {
		duesUpdateScheduler.processAcademyProgramDues(academyId, programId);
	}

	/**
	 * Generate dues for all enrollments in the specified academy.
	 */
	@PostMapping("/generate-academy-dues")
	public ResponseEntity<ServiceResponse> generateDuesForAcademy(@RequestParam String academyId) {
		ServiceResponse response = paymentService.generateDuesForAcademy(academyId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	/**
	 * Mark all successful course payments as PENDING for a given academy.
	 */
	@PutMapping("/mark-academy-pending")
	public ResponseEntity<ServiceResponse> markPaymentsPendingByAcademy(@RequestParam String academyId) {
		ServiceResponse response = paymentService.markPaymentsPendingByAcademy(academyId);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	/**
	 * Update payments from uploaded CSV file to SUCCESS status.
	 */
	@PostMapping("/update-success-academy-from-csv")
	public ResponseEntity<ServiceResponse> updatePaymentsToSuccessOnlyFromCsv(
			@RequestParam("file") MultipartFile csvFile) {
		ServiceResponse response = paymentService.updatePaymentsToSuccessOnlyFromCsv(csvFile);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping(value = "/generate-dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> generateDues(@RequestParam("enrollmentId") String enrollmentId,
			@RequestParam(value = "paymentCategory", defaultValue = "COURSE_FEE", required = false) PaymentCategory paymentCategory) {
		try {
			ServiceResponse response = paymentService.generateDues(enrollmentId);
			return ResponseEntity.status(response.getHttpStatus()).body(response);
		} catch (ResourceException e) {
			ServiceResponse errorResponse = new ServiceResponse();
			errorResponse.setStatus(e.getErrorCodes().getCustomError());
			errorResponse.setMessage(e.getMessage());
			errorResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@GetMapping(value = "/dues/validate", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> generateDuesAndValidate(
			@RequestParam(value = "academyId", required = false) String academyId,
			@RequestParam(value = "courseId", required = false) String courseId,
			@RequestParam(value = "paymentSchedule", required = false) PaymentSchedule paymentSchedule,
			@RequestParam(value = "amount", required = false) Long amount,
			@RequestParam(value = "joiningDate", required = false) LocalDate joiningDate,
			@RequestParam(value = "dueDate", required = false) LocalDate dueDate,
			@RequestParam(value = "absoluteDiscount", required = true) Long absoluteDiscount,
			@RequestParam(value = "enrollmentId", required = false) String enrollmentId) {

		ServiceResponse response = paymentService.generateDuesAndValidate(academyId, courseId, paymentSchedule, amount,
				joiningDate, dueDate, absoluteDiscount, enrollmentId);

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping(value = "/enrollments/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> getPaymentDetails(@PathVariable("enrollmentId") String enrollmentId,
			@RequestParam(value = "paymentCategory", defaultValue = "COURSE_FEE", required = false) PaymentCategory paymentCategory) {
		ServiceResponse response = paymentService.getPaymentDetails(enrollmentId, paymentCategory);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "/enrollments/{enrollmentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> initPayment(@PathVariable("enrollmentId") String enrollmentId,
			@Valid @RequestBody InitPaymentDto initPaymentDto) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		ServiceResponse response = paymentService.initializePayment(currentUser.getUserId(), enrollmentId,
				initPaymentDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PutMapping(value = "/{paymentId}/enrollments/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> updatePayment(@PathVariable("enrollmentId") String enrollmentId,
			@PathVariable("paymentId") String paymentId, @Valid @RequestBody UpdatePaymentDto updatePaymentDto) {
		ServiceResponse response = paymentService.updatePayment(paymentId, enrollmentId, updatePaymentDto, false);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PutMapping(value = "/clear-dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> clearPayment(@RequestParam("enrollmentId") String enrollmentId) {
		ServiceResponse response = paymentService.clearDues(enrollmentId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "/reminders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<PaymentDetailsDto>> sendReminders(
			@Valid @RequestBody List<PaymentReminderDto> paymentReminderDtos) {
		try {
			paymentService.sendPaymentReminders(paymentReminderDtos);
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(Response.<PaymentDetailsDto>builder()
					.status(HttpStatus.ACCEPTED.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<PaymentDetailsDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}
}
