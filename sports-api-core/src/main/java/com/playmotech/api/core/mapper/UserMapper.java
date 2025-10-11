package com.playmotech.api.core.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.Roles;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.UserAddEditDto;
import com.playmotech.api.core.dto.UserProfileAddEditDto;
import com.playmotech.api.core.response.dao.RolesDao;
import com.playmotech.api.core.response.dao.UserProfileDao;

public class UserMapper {
	private UserMapper() {
		super();
	}

	public static UserProfileDao mapUserToUserDao(UserProfile user) {
		UserProfileDao userProfile = new UserProfileDao();
		BeanUtils.copyProperties(user, userProfile, "role");

//		RolesDao roleDao = new RolesDao();
//		if (user.getRole() != null) {
//			roleDao.setRoleName(user.getRole().getRoleName());
//			roleDao.setId(user.getRole().getId());
//			userProfile.setRole(roleDao);
//		}
		// userProfile.setRole(mapUserRoleToUserDaoRole(user.getRole()));

		return userProfile;
	}

	private static RolesDao mapUserRoleToUserDaoRole(Roles role) {
		RolesDao rolesDao = new RolesDao();
		BeanUtils.copyProperties(role, rolesDao);

		return rolesDao;
	}

	public static UserProfile mapUserDtoToUser(UserProfileAddEditDto userProfileDto) {
		UserProfile user = new UserProfile();
		BeanUtils.copyProperties(userProfileDto, user);

		user.setInactive(false);
		return user;
	}

	public static UserProfile mapPlayerDtoToUser(UserAddEditDto userProfileDto) {
		UserProfile user = new UserProfile();
		BeanUtils.copyProperties(userProfileDto, user);

		user.setInactive(false);
		return user;
	}

	public static List<UserProfileDao> convertToResponseList(List<UserProfile> users) {
		if (users == null) {
			return Collections.emptyList();
		}
		List<UserProfileDao> list = new ArrayList<>(users.size());
		for (UserProfile user : users) {
			list.add(mapUserToUserDao(user));
		}
		return list;
	}

}
