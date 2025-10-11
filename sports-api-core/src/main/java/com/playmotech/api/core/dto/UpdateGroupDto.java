package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class UpdateGroupDto {
	private String name;
	private List<String> members;
}
