package com.playmotech.api.core.dto;

import java.util.Map;

import lombok.Data;

@Data
public class ReportResponseDto {
	private Map<String, String> dimension;
	private Long measure;
}
