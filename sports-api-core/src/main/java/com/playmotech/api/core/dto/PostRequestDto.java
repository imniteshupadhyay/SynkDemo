package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Visibility;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostRequestDto {
	// @NotNull(message = "Title cannot be null")
	// @NotEmpty(message = "Title cannot be empty")
	private String title;
	private String body;
	private String mediaUrl;
	@NotNull(message = "visibility cannot be null")
	private Visibility visibility;
	private String academyId;
	private String thumbnailUrl;
}
