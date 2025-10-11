package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.CreateGroupDto;
import com.playmotech.api.core.dto.GroupDto;
import com.playmotech.api.core.dto.UpdateGroupDto;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IGroupService {
	GroupDto createGroup(String academyId, String userId, CreateGroupDto createGroupDto) throws ResourceException;

	List<GroupDto> getGroups(String academyId, String userId) throws ResourceException;

	GroupDto getGroup(String academyId, String groupId) throws ResourceException;

	void addMembersToGroup(String userId, String academyId, String groupId, List<String> members)
			throws ResourceException;

	void removeMembersToGroup(String userId, String academyId, String groupId, List<String> members)
			throws ResourceException;

	void deleteGroup(String userId, String academyId, String groupId) throws ResourceException;

	void deleteGroups(String userId, String academyId, List<String> groupIds) throws ResourceException;

	boolean isAdmin(String userId, String academyId, String groupId) throws ResourceException;

	boolean isMember(String userId, String academyId, String groupId) throws ResourceException;

	void updateGroup(String userId, String academyId, String groupId, UpdateGroupDto updateGroupDto)
			throws ResourceException;
}
