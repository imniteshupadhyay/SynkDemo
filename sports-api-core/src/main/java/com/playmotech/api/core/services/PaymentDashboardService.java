package com.playmotech.api.core.services;

import java.sql.Timestamp;
import java.util.List;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface PaymentDashboardService {

	ServiceResponse getPaymentsList(GenericFilter filter);

	ServiceResponse getPaymentsKpis(GenericFilter filter);

//	ServiceResponse getPaymentTrend(Timestamp startDateLocal, Timestamp endDateLocal, String academyId,
//			List<String> sports, List<String> courseIds, List<String> ageCategories);

	ServiceResponse getCourseEnrollmentDetails(String domainUrl, List<String> academyIds, List<String> courseIds,
			List<String> sports, List<String> ageCategories);

	ServiceResponse getPaymentTrend(String domainUrl, Timestamp startDateLocal, Timestamp endDateLocal,
			List<String> academyIds, List<String> sports, List<String> programIds, List<String> ageCategories);

	ServiceResponse getPendingPaymentDuesList(GenericFilter filter);

	ServiceResponse getDuesPaymentsList(GenericFilter filter);

}
