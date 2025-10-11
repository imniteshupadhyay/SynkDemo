package com.playmotech.api.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class VisibilityConfigDto {
	private String screen;
	private Map<String, Boolean> disableFeatures;
}
