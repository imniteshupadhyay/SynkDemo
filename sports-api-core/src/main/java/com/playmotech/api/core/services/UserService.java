package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.ChangePasswordDto;
import com.playmotech.api.core.dto.FileObjectDetails;
import com.playmotech.api.core.dto.LoginUserDto;
import com.playmotech.api.core.dto.UserProfileAddEditDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface UserService {

	ServiceResponse getUsersList(GenericFilter filter, String userId, String domainUrl);

	ServiceResponse getUserProfileById(String id);

	ServiceResponse addUser(UserProfileAddEditDto userProfileDto, String userId);

	ServiceResponse deleteUser(String id, String userId);

	ServiceResponse editUser(UserProfileAddEditDto userProfileDto, String userId);

	ServiceResponse fileUpload(FileObjectDetails filesToUpload);

	ServiceResponse getUserBasedRoleActions(String userId, String domainUrl);

	ServiceResponse login(LoginUserDto passwordDto);

	ServiceResponse changePassword(ChangePasswordDto passwordDto, String userId);

	ServiceResponse forgotPasswordForApp(String identifier, String userId);

	ServiceResponse forgotPasswordForWeb(String identifier, String userId, String domainUrl);

	ServiceResponse checkUserExistsAndSendOtp(String identifier, Boolean sendOtp, Boolean usePassword,
			String domainUrl) throws ResourceException;

}
