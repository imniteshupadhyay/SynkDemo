package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.EnquiryStatus;

import lombok.Data;

@Data
public class EnquiryRequestDto {
	private String courseId;
	private String description;
	private EnquiryStatus status;
	private String academyId;
	private String notes;
}
