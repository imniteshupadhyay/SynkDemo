package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class ActivityDto {
	private Long id;
	private String name;
	private String description;
	private String category;
	private String subcategory;
}
