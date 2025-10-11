package com.playmotech.api.core.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AcademyLeadDto {
	@NotEmpty(message = "Cannot be empty.")
	private String academyName;
	@NotEmpty(message = "Cannot be empty.")
	private String contactName;
	@NotEmpty(message = "Cannot be empty.")
	private String phoneNumber;
	private String about;
}
