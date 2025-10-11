package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.PaymentCategory;

import lombok.Data;

@Data
public class DiscountRequest {

	private String enrollmentId;
	private String createdById;

	private List<DiscountEntry> discounts;

	@Data
	public static class DiscountEntry {
		private PaymentCategory category; // REGISTRATION, COURSE, etc.
		private Double discountAmount; // Amount
		private String reason;
		private boolean scheduleSpecific; // true if discount applies only to scheduled fees
		private boolean futureUsable; // true if discount is valid for future use
	}

}
