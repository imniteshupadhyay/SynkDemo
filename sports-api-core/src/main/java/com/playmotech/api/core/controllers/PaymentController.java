package com.playmotech.api.core.controllers;

import java.util.List;

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

import com.playmotech.api.core.constants.PaymentCategory;
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

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/payments")
@AllArgsConstructor
public class PaymentController extends BaseController {

//	private final IPaymentService paymentService;

	private final NewPaymentService paymentService;

//	@PostMapping(value = "/enrollments/{enrollmentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<Response<PaymentDto>> initPayment(@PathVariable("enrollmentId") String enrollmentId,
//			@Valid @RequestBody InitPaymentDto initPaymentDto) {
//		try {
//			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
//			return ResponseEntity.status(HttpStatus.OK)
//					.body(Response.<PaymentDto>builder().status(HttpStatus.OK.value()).message("success")
//							.body(paymentService.initPayment(currentUser.getUserId(), enrollmentId, initPaymentDto))
//							.build());
//		} catch (ResourceException e) {
//			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<PaymentDto>builder()
//					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
//		}
//	}
//
//	@PutMapping(value = "/{paymentId}/enrollments/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<Response<PaymentDto>> updatePayment(@PathVariable("enrollmentId") String enrollmentId,
//			@PathVariable("paymentId") String paymentId, @Valid @RequestBody UpdatePaymentDto updatePaymentDto) {
//		try {
//			return ResponseEntity.status(HttpStatus.OK)
//					.body(Response.<PaymentDto>builder().status(HttpStatus.OK.value()).message("success")
//							.body(paymentService.updatePayment(paymentId, enrollmentId, updatePaymentDto)).build());
//		} catch (ResourceException e) {
//			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<PaymentDto>builder()
//					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
//		}
//	}

//	@PostMapping(value = "/enrollments/{enrollmentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<ServiceResponse> webPayment(@PathVariable("enrollmentId") String enrollmentId,
//			@Valid @RequestBody InitPaymentDto initPaymentDto) {
//
//		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
//
//		ServiceResponse response = paymentService.initializePayment(currentUser.getUserId(), enrollmentId,
//				initPaymentDto);
//		return ResponseEntity.status(response.getHttpStatus()).body(response);
//	}

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

	@GetMapping(value = "/enrollments/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> getPaymentDetails(@PathVariable("enrollmentId") String enrollmentId,
			@RequestParam(value = "paymentCategory", defaultValue = "COURSE_FEE", required = false) PaymentCategory paymentCategory) {
		ServiceResponse response = paymentService.getPaymentDetails(enrollmentId, paymentCategory);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping(value = "dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> getDues(@RequestParam("userId") String userId,
			@RequestParam("courseId") String courseId) {
		ServiceResponse response = paymentService.getDues(userId, courseId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping(value = "all-dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> getAllDues(@RequestParam("enrollmentId") String enrollmentId) {
		ServiceResponse response = paymentService.getAllDues(enrollmentId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PutMapping(value = "settle", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> settlePayment(@RequestBody SettlePaymentRequestDto requestDto) {
		ServiceResponse response = paymentService.settlePayment(requestDto);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	public record SettlePaymentRequestDto(String enrollmentId, Long registrationFeePayment,
			Long registrationFeeDiscount, Long courseFeeDiscount, Long overallDiscount, Boolean useForFuture,
			Long coursePaymentAmount) {
	}

	@PutMapping(value = "clear-dues", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> clearPayment(@RequestParam("enrollmentId") String enrollmentId) {
		ServiceResponse response = paymentService.clearDues(enrollmentId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "discount", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ServiceResponse> addDiscount(@RequestBody DiscountRequest request) {
		ServiceResponse response = paymentService.addDiscount(request);
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

//	@GetMapping(value = "/enrollments/{enrollmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<Response<PaymentDetailsDto>> getPaymentDetails(
//			@PathVariable("enrollmentId") String enrollmentId,
//			@RequestParam(value = "paymentCategory", defaultValue = "COURSE_FEE", required = false) PaymentCategory paymentCategory) {
//		try {
//			return ResponseEntity.status(HttpStatus.OK)
//					.body(Response.<PaymentDetailsDto>builder().status(HttpStatus.OK.value()).message("success")
//							.body(paymentService.getPaymentDetails(enrollmentId, paymentCategory)).build());
//		} catch (ResourceException e) {
//			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
//					.body(Response.<PaymentDetailsDto>builder().status(e.getErrorCodes().getCustomError())
//							.message(e.getMessage()).build());
//		}
//	}

//	@PostMapping(value = "/reminders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<Response<PaymentDetailsDto>> sendReminders(
//			@Valid @RequestBody List<PaymentReminderDto> paymentReminderDtos) {
//		try {
//			paymentService.sendPaymentReminders(paymentReminderDtos);
//			return ResponseEntity.status(HttpStatus.ACCEPTED).body(Response.<PaymentDetailsDto>builder()
//					.status(HttpStatus.ACCEPTED.value()).message("success").build());
//		} catch (ResourceException e) {
//			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
//					.body(Response.<PaymentDetailsDto>builder().status(e.getErrorCodes().getCustomError())
//							.message(e.getMessage()).build());
//		}
//	}
}
