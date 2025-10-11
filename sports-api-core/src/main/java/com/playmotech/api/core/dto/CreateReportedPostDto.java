package com.playmotech.api.core.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportedPostDto {
	private String academyId;
	@NotNull(message = "Post Id cannot be null")
	private String postId;
}
