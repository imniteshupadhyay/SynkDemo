package com.playmotech.api.core.response.dao;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentTrendDao {

	private String periodLabel;
	private Long weekNumber;
	private BigDecimal totalAmount;
	private BigDecimal totalRegistrationFeeAmount;
	private BigDecimal totalCourseFeeAmount;

}
