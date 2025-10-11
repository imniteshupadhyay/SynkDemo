package com.playmotech.api.core.services.impl;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.CtaType;
import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.NotificationType;
import com.playmotech.api.core.constants.PushNotifConstants;
import com.playmotech.api.core.dao.GroupMessage;
//import com.playmotech.api.core.dao.Message;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.GroupDto;
import com.playmotech.api.core.dto.GroupMessageDto;
import com.playmotech.api.core.dto.GroupWithMessagesDto;
import com.playmotech.api.core.dto.MessageDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.dynamorepo.GroupMessageRepo;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IGroupMessageService;
import com.playmotech.api.core.services.IGroupService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IStorageService;
import com.playmotech.api.core.services.IUserProfileService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GroupMessageService implements IGroupMessageService {

	private final ModelMapper modelMapper = new ModelMapper();
	private final GroupMessageRepo groupMessageRepo;
	private final IAcademyService academyService;
	private final IUserProfileService userProfileService;
	private final IGroupService groupService;
	private final IStorageService storageService;
	private final IPushNotificationService pushNotificationService;
	@Value("${group-media-base-url}")
	private String groupMediaBaseUrl;

	@Value("${storage.group-media-bucket}")
	private String groupMediaBucket;

	public GroupMessageService(final GroupMessageRepo groupMessageRepo, final IAcademyService academyService,
			final IUserProfileService userProfileService, final IGroupService groupService,
			final IStorageService storageService, final IPushNotificationService pushNotificationService) {
		this.groupMessageRepo = groupMessageRepo;
		this.academyService = academyService;
		this.userProfileService = userProfileService;
		this.groupService = groupService;
		this.storageService = storageService;
		this.pushNotificationService = pushNotificationService;
	}

	@Override
	public MessageDto sendMessage(String userId, String academyId, String groupId, MessageDto messageDto,
			FileObjectDto fileObjectDto) throws ResourceException {
		academyService.getAcademyById(academyId);

		if (!groupService.isAdmin(userId, academyId, groupId)) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User is not authorized to send message.");
		}

		GroupMessage groupMessage = new GroupMessage();
		groupMessage.setAcademyId(academyId);
		groupMessage.setGroupId(groupId);
		groupMessage.setId(UUID.randomUUID().toString());
		groupMessage.setSenderUserId(userId);
		groupMessage.setTime(Instant.now().toEpochMilli());
		groupMessage.setMessage(messageDto.getMessage());

		if (fileObjectDto != null) {
			String prefix = "groups-media/" + groupId + "/" + UUID.randomUUID() + "_"
					+ fileObjectDto.getOriginalFilename();
			storageService.upload(groupMediaBucket, prefix, fileObjectDto.getContent(), fileObjectDto.getContentType());
			groupMessage.setMediaUrl(prefix);
		}

		groupMessageRepo.save(groupMessage);

//        CompletableFuture.runAsync(() -> {
		try {
			GroupDto groupDto = groupService.getGroup(academyId, groupId);
			UserProfileDto userProfileDto = userProfileService.getUserProfileById(userId);
			log.info("Sending push notification for new message in group: {}", groupDto.getName());
			pushNotificationService.sendMessageToTopic(
					String.format(PushNotifConstants.GROUP_SUBSCRIPTION_NAME, groupId),
					NotificationType.LIVE_NOTIFICATION, groupDto.getName(),
					userProfileDto.getDisplayName() + " posted message", "GROUP_DETAILS", CtaType.SCREEN,
					Map.of("groupId", groupId, "academyId", academyId));

		} catch (ResourceException e) {
			log.error("Failed to send push notification for new message in group: " + e.getMessage());
		}
//        });

		MessageDto updated = new MessageDto();
		updated.setMessage(messageDto.getMessage());
		updated.setSenderUserId(userId);
		updated.setTime(groupMessage.getTime());
		updated.setMediaUrl(groupMediaBaseUrl + groupMessage.getMediaUrl());
		return updated;
	}

	@Override
	public List<MessageDto> getGroupMessages(String userId, String academyId, String groupId) throws ResourceException {
		academyService.getAcademyById(academyId);
		if (!groupService.isMember(userId, academyId, groupId) && !groupService.isAdmin(userId, academyId, groupId)) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User is not authorized to view messages.");
		}
		List<GroupMessage> groupMessages = groupMessageRepo.findByAcademyIdAndGroupId(academyId, groupId);
		if (!CollectionUtils.isEmpty(groupMessages)) {
			GroupMessageDto groupMessageDto = new GroupMessageDto();
			groupMessageDto.setId(groupId);
			groupMessageDto.setGroupId(groupId);
			groupMessageDto.setMessages(groupMessages.stream().map(groupMessage -> {
				MessageDto messageDto = new MessageDto();
				messageDto.setMessage(groupMessage.getMessage());
				messageDto.setSenderUserId(groupMessage.getSenderUserId());
				messageDto.setTime(groupMessage.getTime());
				messageDto.setMediaUrl(groupMessage.getMediaUrl());
				return messageDto;
			}).collect(Collectors.toList()));

			List<MessageDto> messageDtos = groupMessageDto.getMessages();
			List<String> userIds = messageDtos.stream().map(MessageDto::getSenderUserId).toList();
			Map<String, UserProfileDto> userProfileDtoMap = userProfileService.getUserProfileByIds(userIds).stream()
					.map(userProfileDto -> new AbstractMap.SimpleEntry<>(userProfileDto.getId(), userProfileDto))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			messageDtos.forEach(messageDto -> {
				UserProfileDto userProfileDto = userProfileDtoMap.get(messageDto.getSenderUserId());
				if (userProfileDto != null) {
					messageDto.setUserProfileMinDto(modelMapper.map(userProfileDto, UserProfileMinDto.class));
				}

				if (messageDto.getMediaUrl() != null) {
					messageDto.setMediaUrl(groupMediaBaseUrl + messageDto.getMediaUrl());
				}
			});
			messageDtos.sort(Comparator.comparing(MessageDto::getTime));
			return messageDtos;
		}
		return Collections.emptyList();
	}

	@Override
	public GroupWithMessagesDto getGroupDetailsWithMessages(String userId, String academyId, String groupId)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		if (!groupService.isMember(userId, academyId, groupId) && !groupService.isAdmin(userId, academyId, groupId)) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User is not authorized to view messages.");
		}
		GroupDto groupDto = groupService.getGroup(academyId, groupId);
		GroupWithMessagesDto groupWithMessagesDto = modelMapper.map(groupDto, GroupWithMessagesDto.class);
		List<GroupMessage> groupMessages = groupMessageRepo.findByAcademyIdAndGroupId(academyId, groupId);
		if (!CollectionUtils.isEmpty(groupMessages)) {
			GroupMessageDto groupMessageDto = new GroupMessageDto();
			groupMessageDto.setId(groupId);
			groupMessageDto.setGroupId(groupId);
			groupMessageDto.setMessages(groupMessages.stream().map(groupMessage -> {
				MessageDto messageDto = new MessageDto();
				messageDto.setMessage(groupMessage.getMessage());
				messageDto.setSenderUserId(groupMessage.getSenderUserId());
				messageDto.setTime(groupMessage.getTime());
				messageDto.setMediaUrl(groupMessage.getMediaUrl());
				return messageDto;
			}).collect(Collectors.toList()));

			List<MessageDto> messageDtos = groupMessageDto.getMessages();
			List<String> userIds = messageDtos.stream().map(MessageDto::getSenderUserId).toList();
			Map<String, UserProfileDto> userProfileDtoMap = userProfileService.getUserProfileByIds(userIds).stream()
					.map(userProfileDto -> new AbstractMap.SimpleEntry<>(userProfileDto.getId(), userProfileDto))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			messageDtos.forEach(messageDto -> {
				UserProfileDto userProfileDto = userProfileDtoMap.get(messageDto.getSenderUserId());
				if (userProfileDto != null) {
					messageDto.setUserProfileMinDto(modelMapper.map(userProfileDto, UserProfileMinDto.class));
				}

				if (messageDto.getMediaUrl() != null) {
					messageDto.setMediaUrl(groupMediaBaseUrl + messageDto.getMediaUrl());
				}
			});
			messageDtos.sort(Comparator.comparing(MessageDto::getTime));
			groupWithMessagesDto.setMessages(messageDtos);
		}
		return groupWithMessagesDto;
	}
}
