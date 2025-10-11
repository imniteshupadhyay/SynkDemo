package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class ModulesActionsRequestDto {
	Long id;
	String actionCode;
	String actionName;
	ModulesRequestDto modules;
	Boolean allowed;
	Boolean disabled;
}
