package com.playmotech.api.core.services.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.dao_postgres.UserProfile;
import com.playmotech.api.core.dto.LoginUserDto;
import com.playmotech.api.core.dto.LoginUserOtpDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.repo.UserProfileRepo;
import com.playmotech.api.core.services.IAuthenticationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthenticationService implements IAuthenticationService {
	private final AuthenticationProvider emailPasswordAuthProvider;
	private final AuthenticationProvider usernameOrEmailOtpAuthProvider;
	private final AuthenticationProvider usernamePasswordAuthenticationProvider;
	private final UserProfileRepo userProfileRepo;

	public AuthenticationService(UserProfileRepo userProfileRepo,
			@Qualifier("emailPasswordAuthProvider") AuthenticationProvider emailPasswordAuthProvider,
			@Qualifier("customAuthProvider") AuthenticationProvider usernameOrEmailOtpAuthProvider,
			@Qualifier("mobilePasswordAuthProvider") AuthenticationProvider usernamePasswordAuthenticationProvider) {
		this.userProfileRepo = userProfileRepo;
		this.emailPasswordAuthProvider = emailPasswordAuthProvider;
		this.usernameOrEmailOtpAuthProvider = usernameOrEmailOtpAuthProvider;
		this.usernamePasswordAuthenticationProvider = usernamePasswordAuthenticationProvider;
	}

	@Override
	@Deprecated
	public UserDetail login(LoginUserDto loginUserDto) throws ResourceException {
		// Determine login method based on provided credentials
		if (loginUserDto.getEmail() != null) {
			if (loginUserDto.getPassword() != null) {
				// Email + Password login
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						loginUserDto.getEmail(), loginUserDto.getPassword());
				emailPasswordAuthProvider.authenticate(authToken);
			} else if (loginUserDto.getOtp() != null) {
				// Email + OTP login
				log.info("OTP received while logging in: {}", loginUserDto.getOtp());
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						loginUserDto.getEmail(), loginUserDto.getOtp());
				usernameOrEmailOtpAuthProvider.authenticate(authToken);
			} else {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid login credentials");
			}
		} else if (loginUserDto.getUsername() != null && loginUserDto.getOtp() != null) {
			// Username + OTP login
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					loginUserDto.getUsername(), loginUserDto.getOtp());
			usernameOrEmailOtpAuthProvider.authenticate(authToken);
		} else {
			throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid login credentials");
		}

		// If we reach here, authentication was successful
		Optional<UserProfile> userProfile;
		if (loginUserDto.getUsername() != null) {
			userProfile = userProfileRepo.findByUsername(loginUserDto.getUsername());
		} else {
			userProfile = userProfileRepo.findByEmailId(loginUserDto.getEmail());
		}

		if (userProfile.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
		}

		userProfile.get().getAuthDetails().setOtpUsed(true);
		userProfileRepo.save(userProfile.get());
		return new UserDetail(userProfile.get().getId(), userProfile.get().getUsername(),
				userProfile.get().getAuthDetails().getOtpHashed());
	}

	@Override
	public UserDetail newLogin(LoginUserDto loginUserDto) throws ResourceException {
		// Determine Login method based on provided credentials
		if (loginUserDto.getEmail() != null) {
			if (loginUserDto.getPassword() != null) {
				// Email + Password login
				log.info("User logging via email + password flow");
				UsernamePasswordAuthenticationToken emailPassAuthToken = new UsernamePasswordAuthenticationToken(
						loginUserDto.getEmail(), loginUserDto.getPassword());
				emailPasswordAuthProvider.authenticate(emailPassAuthToken);
			} else if (loginUserDto.getOtp() != null) {
				// Email + OTP login
				log.info("User logging in via email + OTP flow");
				log.info("OTP received while logging in: {}", loginUserDto.getOtp());
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						loginUserDto.getEmail(), loginUserDto.getOtp());
				usernameOrEmailOtpAuthProvider.authenticate(authToken);
			} else {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid login credentails");
			}
		} else if (loginUserDto.getUsername() != null) {
			if (loginUserDto.getOtp() != null) {
				log.info("User logging in via username + OTP flow");
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						loginUserDto.getUsername(), loginUserDto.getOtp());
				usernameOrEmailOtpAuthProvider.authenticate(authToken);
			} else if (loginUserDto.getPassword() != null) {
				log.info("User logging in via username + password flow");
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						loginUserDto.getUsername(), loginUserDto.getPassword());
				usernamePasswordAuthenticationProvider.authenticate(authToken);
			} else {
				throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid login credentials");
			}
		} else {
			throw new ResourceException(ErrorCodes.INVALID_REQUEST, "Invalid login credentials");
		}
		// If we reach here, authentication is success
		Optional<UserProfile> userProfile;
		if(loginUserDto.getUsername() != null) {
//			userProfile = userProfileRepo.findByUsername(loginUserDto.getUsername());
			userProfile = userProfileRepo.findByUsernameAndPrimaryAccountIsTrue(loginUserDto.getUsername());
		} else {
//			userProfile = userProfileRepo.findByEmailId(loginUserDto.getEmail());
			userProfile = userProfileRepo.findByEmailIdAndPrimaryAccountIsTrue(loginUserDto.getEmail());
		}

		if (userProfile.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
		}
		
		userProfile.get().getAuthDetails().setOtpUsed(true);
		userProfileRepo.save(userProfile.get());
		return new UserDetail(userProfile.get().getId(), userProfile.get().getUsername(), userProfile.get().getAuthDetails().getOtpHashed());
	}

	@Override
	public UserDetail loginViaOtp(LoginUserOtpDto loginUserOtpDto) throws ResourceException {
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
				loginUserOtpDto.getUsername(), loginUserOtpDto.getOtp());

		usernameOrEmailOtpAuthProvider.authenticate(authToken);

		Optional<UserProfile> userProfile = userProfileRepo.findByUsername(loginUserOtpDto.getUsername());
		if (userProfile.isEmpty()) {
			throw new ResourceException(ErrorCodes.RESOURCE_NOT_FOUND, "User not found");
		}

		return new UserDetail(userProfile.get().getId(), userProfile.get().getUsername(),
				userProfile.get().getAuthDetails().getPasswordHashed());
	}
}
