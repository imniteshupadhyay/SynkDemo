package com.playmotech.api.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCoachToAcademyRequestDto {
	@NotNull(message = "coachUserId cannot be null")
	private String coachUserId;
	private String designation;
	private Integer experienceInMonths;
	private Integer roleId;
}
