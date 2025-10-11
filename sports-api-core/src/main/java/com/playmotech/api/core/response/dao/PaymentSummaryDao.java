package com.playmotech.api.core.response.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummaryDao {
	private Long enrollmentId;
	private Long traineeUserId;
	private Long courseId;
	private Long academyId;
	private Double totalExpectedPayment;
	private Double totalPaid;
	private Double pendingAmount;
}
