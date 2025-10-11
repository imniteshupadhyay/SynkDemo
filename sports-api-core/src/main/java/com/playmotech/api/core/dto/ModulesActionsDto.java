package com.playmotech.api.core.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModulesActionsDto {
	private Long id;
	private String module;
	private List<ActionsDto> actions = new ArrayList<>();

	// !Additional constructor for JPQL - do not remove this
	public ModulesActionsDto(Long id, String module, ActionsDto action) {
		this.id = id;
		this.module = module;
		actions.add(action);
	}
}
