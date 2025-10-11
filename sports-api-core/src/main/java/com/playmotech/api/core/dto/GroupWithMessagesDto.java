package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class GroupWithMessagesDto {
	private String id;
	private String name;
	private List<UserProfileMinDto> members;
	private List<String> adminUserIds;
	private String lastMessage;
	private List<MessageDto> messages;
}
