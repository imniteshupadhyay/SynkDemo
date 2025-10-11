package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class RoleByIdResponseDto {
	private String roleName;
	private String description;
	private boolean isEditable;
	private Long sequence;
	private List<ModulesActionsDto> modulesActions;
}
