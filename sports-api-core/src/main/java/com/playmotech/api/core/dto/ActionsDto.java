package com.playmotech.api.core.dto;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ActionsDto {
	private Long id;
	private String actionName;
	private String actionCode;
	private Boolean disabled;
	private boolean isAllowed;

	public ActionsDto(Long id, String actionName, String actionCode, Boolean disabled) {
		this.id = id;
		this.actionName = actionName;
		this.actionCode = actionCode;
		this.disabled = disabled;
	}
}
