package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.MediaType;

import lombok.Data;

@Data
public class MediaDto {
	private Long id;
	private String urls;
	private MediaType mediaType;
}
