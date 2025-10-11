package com.playmotech.api.core.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class FileObjectDetails {
	private MultipartFile file;
	private String documentType;
}
