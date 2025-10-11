package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class PaymentDueDto {
	private String traineeUserId;
	private UserProfileMinDto userProfile;
	private String pendingAmount;
}
