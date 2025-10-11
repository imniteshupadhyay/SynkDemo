package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.AttributeType;

import lombok.Data;

@Data
public class Attribute {
	private String name;
	private AttributeType type;
	private List<String> options;
	private boolean required;
	private String value;
}
