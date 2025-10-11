package com.playmotech.api.core.dto;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class FileObjectDto {
	private byte[] content;
	private String contentType;
	private String originalFilename;
}
