package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class Category {
	private String category;
	private List<Attribute> attributes;
	private String categoryRating;
}
