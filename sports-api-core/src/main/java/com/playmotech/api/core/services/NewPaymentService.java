package com.playmotech.api.core.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.controllers.PaymentController.SettlePaymentRequestDto;
import com.playmotech.api.core.dao_postgres.Payment;
import com.playmotech.api.core.dto.CoursePaymentDetailsDto;
import com.playmotech.api.core.dto.DiscountRequest;
import com.playmotech.api.core.dto.InitPaymentDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.PaymentDueDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.dto.UpdatePaymentDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;

public interface NewPaymentService {

	ServiceResponse initializePayment(String userId, String enrollmentId, InitPaymentDto initPaymentDto);

	ServiceResponse updatePayment(String paymentId, String enrollmentId, UpdatePaymentDto updatePaymentDto,
			boolean dataMigrate);

	ServiceResponse getPaymentDetails(String enrollmentId, PaymentCategory paymentCategory);

	List<CoursePaymentDetailsDto> getCoursePaymentDetails(String academyId, String courseId, PaymentCategory category)
			throws ResourceException;

	List<Payment> getPaymentsBetween(String academyId, String startDate, String endDate, List<String> courseIds)
			throws ResourceException;

	List<PaymentDueDto> getPaymentDueDto(String academyId, PaymentCategory paymentCategory) throws ResourceException;

	void sendPaymentReminders(List<PaymentReminderDto> paymentReminderDtos) throws ResourceException;

	ServiceResponse generateDues(String enrollmentId) throws ResourceException;

	ServiceResponse generateDuesAndValidate(String academyId, String courseId, PaymentSchedule paymentSchedule,
			Long amount, LocalDate joiningDate, LocalDate dueDate, Long absoluteDiscount, String enrollmentId);

	List<PaymentDetailsDto> getPaymentDetailsByAcademy(String academyId, List<String> courseIds,
			PaymentCategory category) throws ResourceException;

	ServiceResponse updateDues(String enrollmentId, LocalDate todayDate);

	ServiceResponse getDues(String userId, String courseId);

	ServiceResponse clearDues(String enrollmentId);

	ServiceResponse getLedgers(String enrollmentId);

	ServiceResponse getAllDues(String enrollmentId);

	ServiceResponse settlePayment(SettlePaymentRequestDto requestDto);

	ServiceResponse addDiscount(DiscountRequest request);

	ServiceResponse generateBulkDues();

	ServiceResponse generateDuesForEnrollment(String enrollmentId);

	ServiceResponse updatePaymentsToSuccessOnly(List<String> paymentIds, String enrollmentId);

	ServiceResponse markPaymentsPendingOnly(String enrollmentId);

	ServiceResponse generateDuesForAcademy(String academyId);

	ServiceResponse markPaymentsPendingByAcademy(String academyId);

	ServiceResponse updatePaymentsToSuccessOnlyFromCsv(MultipartFile csvFile);

}
