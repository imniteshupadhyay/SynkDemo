package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.LoginUserDto;
import com.playmotech.api.core.dto.LoginUserOtpDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IAuthenticationService {
	@Deprecated
	UserDetail login(LoginUserDto loginUserDto) throws ResourceException;

	UserDetail newLogin(LoginUserDto loginUserDto) throws ResourceException;

	UserDetail loginViaOtp(LoginUserOtpDto loginUserOtpDto) throws ResourceException;

}
