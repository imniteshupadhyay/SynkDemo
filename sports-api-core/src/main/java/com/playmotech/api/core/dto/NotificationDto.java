package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.CtaType;

import lombok.Data;

@Data
public class NotificationDto {
	private String notificationTimeInUtc;
	private String notification;
	private CtaType ctaType;
	private String cta;
	private Map<String, String> extraArgs;
}
