package com.playmotech.api.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class NotificationDataDto {
	private String ctaScreen;
	private Map<String, String> params;
}
