package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class RolesRequestDto {
	Long id;
	String roleName;
	String description;
	boolean isEditable;
	Long sequence;
	List<ModulesActionsRequestDto> modulesActions;
}
