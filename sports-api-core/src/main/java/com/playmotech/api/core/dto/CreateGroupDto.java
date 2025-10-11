package com.playmotech.api.core.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGroupDto {
	@NotNull(message = "Name be empty.")
	@NotEmpty(message = "Name be empty.")
	private String name;
	@NotNull(message = "Members cannot be empty.")
	private List<String> members;
	private List<String> adminUserIds;
}
