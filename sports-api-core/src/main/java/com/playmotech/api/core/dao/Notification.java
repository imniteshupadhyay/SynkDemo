package com.playmotech.api.core.dao;

import java.util.Map;

import com.playmotech.api.core.constants.CtaType;

import lombok.Data;

@Data
public class Notification {
	private long epochTimeInMillis;
	private String notification;
	private CtaType ctaType;
	private String cta;
	private Map<String, String> extraArgs;
}
