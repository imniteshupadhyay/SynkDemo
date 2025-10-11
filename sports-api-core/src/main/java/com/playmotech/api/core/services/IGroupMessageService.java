package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.GroupWithMessagesDto;
import com.playmotech.api.core.dto.MessageDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IGroupMessageService {
	MessageDto sendMessage(String userId, String academyId, String groupId, MessageDto messageDto,
			FileObjectDto fileObjectDto) throws ResourceException;

	List<MessageDto> getGroupMessages(String userId, String academyId, String groupId) throws ResourceException;

	GroupWithMessagesDto getGroupDetailsWithMessages(String userId, String academyId, String groupId)
			throws ResourceException;
}
