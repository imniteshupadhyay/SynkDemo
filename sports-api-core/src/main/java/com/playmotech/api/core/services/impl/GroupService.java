package com.playmotech.api.core.services.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.PushNotifConstants;
import com.playmotech.api.core.dao_postgres.Academy;
import com.playmotech.api.core.dao_postgres.Group;
import com.playmotech.api.core.dao_postgres.GroupAdminMapping;
import com.playmotech.api.core.dao_postgres.GroupMemberMapping;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.CreateGroupDto;
import com.playmotech.api.core.dto.GroupDto;
import com.playmotech.api.core.dto.UpdateGroupDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.GroupMemberMappingRepo;
import com.playmotech.api.core.repo.GroupRepo;
import com.playmotech.api.core.services.IAcademyService;
import com.playmotech.api.core.services.IGroupService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IUserProfileService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GroupService implements IGroupService {

	private final ModelMapper modelMapper = new ModelMapper();
	private final GroupRepo groupRepo;
	private final IAcademyService academyService;
	private final IUserProfileService userProfileService;
	private final IPushNotificationService pushNotificationService;
	private final GroupMemberMappingRepo groupMemberMappingRepo;

	@Autowired
	public GroupService(final GroupRepo groupRepo, final IAcademyService academyService,
			final IUserProfileService userProfileService, final IPushNotificationService pushNotificationService,
			final GroupMemberMappingRepo groupMemberMappingRepo) {
		this.groupRepo = groupRepo;
		this.academyService = academyService;
		this.userProfileService = userProfileService;
		this.pushNotificationService = pushNotificationService;
		this.groupMemberMappingRepo = groupMemberMappingRepo;
	}

	@Override
	public GroupDto createGroup(String academyId, String userId, CreateGroupDto createGroupDto)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		Group group = modelMapper.map(createGroupDto, Group.class);
		String groupId = UUID.randomUUID().toString();
		group.setId(groupId);
		group.setCreatedOn(Timestamp.from(Instant.now()));
		group.setCreatedByUserProfile(UserProfile.builder().id(userId).build());
		group.setInactive(false);
		group.setGroupAdminMappings(List.of(GroupAdminMapping.builder().group(Group.builder().id(groupId).build())
				.groupAdminUserProfile(UserProfile.builder().id(userId).build())
				.createdOn(Timestamp.from(Instant.now())).build()));
		group.setAcademy(Academy.builder().id(academyId).build());
		createGroupDto.getMembers().add(userId);
		group.setGroupMemberMappings(buildGroupMemberMappings(groupId, createGroupDto.getMembers()));
		groupRepo.save(group);
		subscribeToGroupNotificationTopic(groupId,
				group.getGroupMemberMappings().stream().map(GroupMemberMapping::getGroupMemberUserProfile).toList());
		return getGroup(academyId, groupId);
	}

	@Override
	public List<GroupDto> getGroups(String academyId, String userId) throws ResourceException {
		academyService.getAcademyById(academyId);
		List<Group> groups = groupRepo.findByAcademy_Id(academyId).stream().filter(group -> !group.getInactive())
				.toList();
		groups = groups.stream()
				.filter(group -> !CollectionUtils.isEmpty(group.getGroupMemberMappings())
						&& group.getGroupMemberMappings().stream().anyMatch(groupMemberMapping -> groupMemberMapping
								.getGroupMemberUserProfile().getId().equalsIgnoreCase(userId)))
				.toList();
		if (!CollectionUtils.isEmpty(groups)) {
			return groups.stream().map(group -> {
				try {
					return convertFromGroup(group);
				} catch (ResourceException e) {
					log.error("Error while converting group to groupDto: {}", e.getMessage());
				}
				return null;
			}).toList();
		}
		return List.of();
	}

	@Override
	public GroupDto getGroup(String academyId, String groupId) throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		return convertFromGroup(group.get());
	}

	@Override
	public void addMembersToGroup(String userId, String academyId, String groupId, List<String> members)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		if (CollectionUtils.isEmpty(group.get().getGroupAdminMappings())
				|| group.get().getGroupAdminMappings().stream().noneMatch(groupAdminMapping -> groupAdminMapping
						.getGroupAdminUserProfile().getId().equalsIgnoreCase(userId))) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User is not authorized to add members to group.");
		}
		group.get().getGroupMemberMappings().addAll(buildGroupMemberMappings(groupId, members));

		Group updatedGroup = groupRepo.save(group.get());
		subscribeToGroupNotificationTopic(groupId,
				updatedGroup.getGroupMemberMappings().stream().map(GroupMemberMapping::getGroupMemberUserProfile)
						.filter(groupMemberUserProfile -> members.contains(groupMemberUserProfile.getId())).toList());
	}

	@Override
	public void removeMembersToGroup(String userId, String academyId, String groupId, List<String> members)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		if (CollectionUtils.isEmpty(group.get().getGroupAdminMappings())
				|| group.get().getGroupAdminMappings().stream().noneMatch(groupAdminMapping -> groupAdminMapping
						.getGroupAdminUserProfile().getId().equalsIgnoreCase(userId))) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED,
					"User is not authorized to remove members from group.");
		}
		List<GroupMemberMapping> toBeRemoved = group.get().getGroupMemberMappings().stream()
				.filter(groupMemberMapping -> members.contains(groupMemberMapping.getGroupMemberUserProfile().getId()))
				.toList();
		group.get().getGroupMemberMappings().removeAll(toBeRemoved);
		unsubscribeToGroupNotificationTopic(groupId, members);
		groupRepo.save(group.get());
	}

	@Override
	public void deleteGroup(String userId, String academyId, String groupId) throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		if (CollectionUtils.isEmpty(group.get().getGroupAdminMappings())
				|| group.get().getGroupAdminMappings().stream().noneMatch(groupAdminMapping -> groupAdminMapping
						.getGroupAdminUserProfile().getId().equalsIgnoreCase(userId))) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User is not authorized to delete group.");
		}
		group.get().setInactive(true);
		groupRepo.save(group.get());
	}

	@Override
	public void deleteGroups(String userId, String academyId, List<String> groupIds) throws ResourceException {
		for (String groupId : groupIds) {
			academyService.getAcademyById(academyId);
			Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
			if (group.isEmpty() || CollectionUtils.isEmpty(group.get().getGroupAdminMappings())
					|| group.get().getGroupAdminMappings().stream().noneMatch(groupAdminMapping -> groupAdminMapping
							.getGroupAdminUserProfile().getId().equalsIgnoreCase(userId))) {
				continue;
			}
			group.get().setInactive(true);
			groupRepo.save(group.get());
		}
	}

	@Override
	public boolean isAdmin(String userId, String academyId, String groupId) throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		return !CollectionUtils.isEmpty(group.get().getGroupAdminMappings())
				&& group.get().getGroupAdminMappings().stream().anyMatch(groupAdminMapping -> groupAdminMapping
						.getGroupAdminUserProfile().getId().equalsIgnoreCase(userId));
	}

	@Override
	public boolean isMember(String userId, String academyId, String groupId) throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		return !CollectionUtils.isEmpty(group.get().getGroupMemberMappings())
				&& group.get().getGroupMemberMappings().stream().anyMatch(groupMemberMapping -> groupMemberMapping
						.getGroupMemberUserProfile().getId().equalsIgnoreCase(userId));
	}

	@Transactional
	@Override
	public void updateGroup(String userId, String academyId, String groupId, UpdateGroupDto updateGroupDto)
			throws ResourceException {
		academyService.getAcademyById(academyId);
		Optional<Group> group = groupRepo.findByAcademy_IdAndId(academyId, groupId);
		if (group.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "Group not found.");
		}
		if (CollectionUtils.isEmpty(group.get().getGroupAdminMappings())
				|| group.get().getGroupAdminMappings().stream().noneMatch(groupAdminMapping -> groupAdminMapping
						.getGroupAdminUserProfile().getId().equalsIgnoreCase(userId))) {
			throw new ResourceException(ErrorCodes.UNAUTHORIZED, "User is not authorized to update group.");
		}
		List<GroupMemberMapping> existingMembers = group.get().getGroupMemberMappings();
		List<GroupMemberMapping> newMembers = buildGroupMemberMappings(groupId, updateGroupDto.getMembers());
		groupMemberMappingRepo.deleteAll(existingMembers);
		Group updatedGroup = modelMapper.map(updateGroupDto, Group.class);
		if (!StringUtils.isEmpty(updatedGroup.getName())) {
			group.get().setName(updatedGroup.getName());
		}

		try {
//        group.get().setGroupMemberMappings(newMembers);
			List<GroupMemberMapping> modifiableNewMembers = new ArrayList<>(newMembers);
			if (newMembers.stream().noneMatch(groupMemberMapping -> groupMemberMapping.getGroupMemberUserProfile()
					.getId().equalsIgnoreCase(userId))) {
				GroupMemberMapping groupMemberMapping = GroupMemberMapping.builder()
						.group(Group.builder().id(groupId).build())
						.groupMemberUserProfile(UserProfile.builder().id(userId).build())
						.createdOn(Timestamp.from(Instant.now())).build();
//                List<GroupMemberMapping> memberMappings = new ArrayList<>(newMembers);
				modifiableNewMembers.add(groupMemberMapping);
				group.get().setGroupMemberMappings(modifiableNewMembers);
			} else {
				group.get().setGroupMemberMappings(modifiableNewMembers);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		groupRepo.save(group.get());

		if (!CollectionUtils.isEmpty(newMembers)) {
			List<GroupMemberMapping> removedMembers = new ArrayList<>(existingMembers); // Ensure mutable list
			removedMembers.removeAll(newMembers);
			if (!removedMembers.isEmpty()) {
				unsubscribeToGroupNotificationTopic(groupId, removedMembers.stream()
						.map(groupMemberMapping -> groupMemberMapping.getGroupMemberUserProfile().getId()).toList());
			}
		}

		if (!CollectionUtils.isEmpty(newMembers)) {
			List<GroupMemberMapping> addedMembers = new ArrayList<>(newMembers); // Ensure mutable list
			addedMembers.removeAll(existingMembers);
			if (!addedMembers.isEmpty()) {
				subscribeToGroupNotificationTopic(groupId,
						newMembers.stream().map(GroupMemberMapping::getGroupMemberUserProfile).toList());
			}
		}
	}

	private GroupDto convertFromGroup(Group group) throws ResourceException {
		GroupDto groupDto = modelMapper.map(group, GroupDto.class);
		if (!CollectionUtils.isEmpty(group.getGroupMemberMappings())) {
			List<UserProfile> userProfileDtos = group.getGroupMemberMappings().stream()
					.map(GroupMemberMapping::getGroupMemberUserProfile).toList();
			groupDto.setMembers(userProfileDtos.stream()
					.map(userProfileDto -> modelMapper.map(userProfileDto, UserProfileMinDto.class)).toList());
		}

		if (!CollectionUtils.isEmpty(group.getGroupAdminMappings())) {
			List<UserProfile> userProfileDtos = group.getGroupAdminMappings().stream()
					.map(GroupAdminMapping::getGroupAdminUserProfile).toList();
			groupDto.setAdminUserIds(userProfileDtos.stream().map(UserProfile::getId).toList());
		}
		return groupDto;
	}

	private void subscribeToGroupNotificationTopic(String groupId, List<UserProfile> userProfileDtos)
			throws ResourceException {
		String topicName = String.format(PushNotifConstants.GROUP_SUBSCRIPTION_NAME, groupId);
		for (UserProfile userProfileDto : userProfileDtos) {
			log.info("Subscribing user {} to topic {}", userProfileDto.getId(), topicName);
			pushNotificationService.subscribeToTopic(userProfileDto.getAndroidFcmPushToken(), topicName);
		}
	}

	private void unsubscribeToGroupNotificationTopic(String groupId, List<String> userIds) throws ResourceException {
		String topicName = String.format(PushNotifConstants.GROUP_SUBSCRIPTION_NAME, groupId);
		List<UserProfileDto> userProfileDtos = userProfileService.getUserProfileByIds(userIds).stream()
				.filter(userProfileDto -> !StringUtils.isEmpty(userProfileDto.getAndroidFcmPushToken())).toList();
		for (UserProfileDto userProfileDto : userProfileDtos) {
			log.info("Unsubscribing user {} from topic {}", userProfileDto.getId(), topicName);
			pushNotificationService.subscribeToTopic(userProfileDto.getAndroidFcmPushToken(), topicName);
		}
	}

	private List<GroupMemberMapping> buildGroupMemberMappings(String groupId, List<String> memberIds) {
		return memberIds.stream()
				.map(memberId -> GroupMemberMapping.builder().group(Group.builder().id(groupId).build())
						.groupMemberUserProfile(UserProfile.builder().id(memberId).build())
						.createdOn(Timestamp.from(Instant.now())).build())
				.toList();
	}
}
