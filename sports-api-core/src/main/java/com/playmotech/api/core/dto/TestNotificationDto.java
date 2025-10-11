package com.playmotech.api.core.dto;

import java.util.Map;

import com.playmotech.api.core.constants.CtaType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestNotificationDto {
	@NotNull(message = "Title cannot be null")
	private String title;
	@NotNull(message = "Body cannot be null")
	private String body;
	@NotNull(message = "Cta cannot be null")
	private String cta;
	@NotNull(message = "CtaType cannot be null")
	private CtaType ctaType;
	private Map<String, String> extraParams;
}
