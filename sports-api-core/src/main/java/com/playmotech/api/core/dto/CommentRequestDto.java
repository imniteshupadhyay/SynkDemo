package com.playmotech.api.core.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequestDto {
	@NotNull(message = "Text cannot be null")
	@NotEmpty(message = "Text cannot be empty")
	private String text;
}
