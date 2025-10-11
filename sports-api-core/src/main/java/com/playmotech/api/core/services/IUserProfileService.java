package com.playmotech.api.core.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.LoginResponseDto;
import com.playmotech.api.core.dto.UpdateUserProfileDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.dto.UserStatsDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.dao.UserProfileDetails;

/**
 * Created By: deep.patel
 **/
public interface IUserProfileService {
	UserProfileDto create(UserProfileDto resource, Boolean sendOtp) throws ResourceException;

	UserProfileDto update(String userId, UpdateUserProfileDto resource) throws ResourceException;

	String updateProfilePicture(String userId, FileObjectDto fileObjectDto) throws ResourceException;

	Optional<UserDetail> userDetail(String username);

	UserProfileDto getUserProfileByUsername(String username) throws ResourceException;

	List<UserProfileDto> getUserProfileDtoListByUsername(String username) throws ResourceException;

	List<UserProfileMinDto> searchUser(String searchTxt) throws ResourceException;

	List<UserProfileDto> getUserProfileByIds(List<String> userIds) throws ResourceException;

	UserProfileDto getUserProfileById(String userId) throws ResourceException;

	List<UserProfileDto> getByUserType(UserType userType, String searchTxt) throws ResourceException;

	void exists(String username) throws ResourceException;

	List<UserProfileDto> getUserProfileByIdsAndNameAndPhoneNumber(List<String> userIds, String name, String phoneNumber,
			String searchTxt) throws ResourceException;

	String sendOtp(String phoneNumber) throws ResourceException;

	void deleteUser(String userId) throws ResourceException;

	boolean checkUserExistBeforeOtpForWeb(String domainUrl, String phoneNumber) throws ResourceException;

	UserProfile getByUsernameOrEmail(String username) throws ResourceException;

	UserProfile getByIdentifier(String identifier) throws ResourceException;

	/**
	 * Switches the user profile to another account associated with the same phone
	 * number
	 * 
	 * @param currentUser    The current user details
	 * @param switchToUserId The user ID to switch to
	 * @return LoginResponseDto of the switched account
	 * @throws ResourceException if the operation fails
	 */
	LoginResponseDto switchProfile(UserDetail currentUser, String switchToUserId) throws ResourceException;

	/**
	 * Switches the primary account designation to another profile associated with
	 * the same phone number
	 * 
	 * @param currentUser The current user details
	 * @param userId      The user ID to set as primary (if null, current user will
	 *                    be set as primary)
	 * @return List<UserProfileDto> which holds the users object associated with the
	 *         same phone number
	 * @throws ResourceException if the operation fails
	 */
	List<UserProfileDto> switchPrimaryProfile(UserDetail currentUser, String userId) throws ResourceException;

	Optional<UserDetail> userById(String id);

	List<UserProfileDetails> getUsersByUsername(String username, String academyDomain);

	UserStatsDto getUserStats(String userId) throws ResourceException;

	Map<String, UserStatsDto> getUsersStats(List<String> userId) throws ResourceException;

	List<BadmintonMatchDto> getUserMatches(String userId) throws ResourceException;
}
