package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;

import com.playmotech.api.core.dao_postgres.Organisation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDao {
	private Long id;
	private String name;
	private String description;
	private String category;
	private String subcategory;
	private Organisation organisation;
	private LocalDateTime insertedOn;
	private String createdBy;
}
