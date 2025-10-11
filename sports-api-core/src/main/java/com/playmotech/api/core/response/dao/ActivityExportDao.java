package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityExportDao {
	private Long id;
	private String name;
	private String description;
	private String category;
	private String subcategory;
	private LocalDateTime insertedOn;
	private String createdByName;
}
