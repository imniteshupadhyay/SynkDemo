package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class MessageDto {
	private Long time;
	private String senderUserId;
	private UserProfileMinDto userProfileMinDto;
	private String message;
	private String mediaUrl;
}
