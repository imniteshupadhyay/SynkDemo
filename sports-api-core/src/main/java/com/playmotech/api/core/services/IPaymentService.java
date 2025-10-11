package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.dao_postgres.Payment;
import com.playmotech.api.core.dto.CoursePaymentDetailsDto;
import com.playmotech.api.core.dto.InitPaymentDto;
import com.playmotech.api.core.dto.PaymentDetailsDto;
import com.playmotech.api.core.dto.PaymentDto;
import com.playmotech.api.core.dto.PaymentDueDto;
import com.playmotech.api.core.dto.PaymentReminderDto;
import com.playmotech.api.core.dto.UpdatePaymentDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IPaymentService {
	PaymentDto initPayment(String userId, String enrollmentId, InitPaymentDto initPaymentDto) throws ResourceException;

	PaymentDto updatePayment(String paymentId, String enrollmentId, UpdatePaymentDto updatePaymentDto)
			throws ResourceException;

	PaymentDetailsDto getPaymentDetails(String enrollmentId, PaymentCategory paymentCategory) throws ResourceException;

	List<CoursePaymentDetailsDto> getCoursePaymentDetails(String academyId, String courseId, PaymentCategory category)
			throws ResourceException;

	List<Payment> getPaymentsBetween(String academyId, String startDate, String endDate, List<String> courseIds)
			throws ResourceException;

	List<PaymentDetailsDto> getPaymentDetailsByAcademy(String academyId, List<String> courseIds)
			throws ResourceException;

	List<PaymentDueDto> getPaymentDueDto(String academyId) throws ResourceException;

	void sendPaymentReminders(List<PaymentReminderDto> paymentReminderDtos) throws ResourceException;
}
