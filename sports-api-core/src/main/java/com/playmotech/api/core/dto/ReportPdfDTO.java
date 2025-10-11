package com.playmotech.api.core.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ReportPdfDTO {

	@JsonProperty("BADMINTON")
	private List<Category> badminton;

	@JsonProperty("BADMINTON")
	private List<Category> cricket;

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Category {
		private String category;
		private List<Attribute> attributes;
		private String categoryRating;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Attribute {
		private String name;
		private String type;
		private String value;
		private List<String> options;
		private Boolean required;
	}
}
