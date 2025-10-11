package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Mail {
	private String subject;

	private String message;

	private String recipient;

	private String sender;

	private String ccRecipient;

	private String bccRecipient;

	private List<String> attachments;

	private Map<String, Object> model;

	private String templateName;
}
