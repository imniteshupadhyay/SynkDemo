package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.constants.ErrorCodes;
import com.playmotech.api.core.constants.UserType;
import com.playmotech.api.core.dto.BadmintonMatchDto;
import com.playmotech.api.core.dto.ChangePasswordDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.LoginResponseDto;
import com.playmotech.api.core.dto.LoginUserDto;
import com.playmotech.api.core.dto.NotificationDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.TestNotificationDto;
import com.playmotech.api.core.dto.UpdateUserProfileDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.dto.UserExistsDto;
import com.playmotech.api.core.dto.UserProfileDto;
import com.playmotech.api.core.dto.UserProfileMinDto;
import com.playmotech.api.core.dto.UserStatsDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.UserProfileDetails;
import com.playmotech.api.core.security.JwtService;
import com.playmotech.api.core.services.IPushNotificationService;
import com.playmotech.api.core.services.IUserProfileService;
import com.playmotech.api.core.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By: deep.patel
 **/

@Slf4j
@RestController
@RequestMapping("/users")
@CrossOrigin("*")
public class UserController extends BaseController {

	private final IUserProfileService userProfileService;
	private final JwtService jwtService;
	private final IPushNotificationService notificationService;

	private final UserService userService;

	@Value("${otp-bypass}")
	private Boolean otpByPass;

	@Value("${otp-bypass-username}")
	private List<String> otpByPassUsernames;

	@Value("${otp-bypass-email}")
	private List<String> otpByPassEmails;

	public UserController(final IUserProfileService userProfileService, final JwtService jwtService,
			final IPushNotificationService notificationService, UserService userService) {
		this.userProfileService = userProfileService;
		this.jwtService = jwtService;
		this.notificationService = notificationService;
		this.userService = userService;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<UserProfileDto>> create(@RequestBody @Valid UserProfileDto userProfileDto,
			@RequestParam(value = "sendotp", defaultValue = "false") Boolean sendOtp) {
		UserProfileDto updatedUserProfileDto;
		try {
			updatedUserProfileDto = userProfileService.create(userProfileDto, sendOtp);
		} catch (ResourceException e) {
			log.error("Failed to save User Profile.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<UserProfileDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(Response.<UserProfileDto>builder()
				.status(HttpStatus.CREATED.value()).message("success").body(updatedUserProfileDto).build());
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<List<UserProfileDto>>> getUserprofile(@RequestParam("userType") UserType userType,
			@RequestParam(value = "t", required = false) String searchTxt) {
		try {
			List<UserProfileDto> userProfileDtos = userProfileService.getByUserType(userType, searchTxt);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<UserProfileDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(userProfileDtos).build());
		} catch (ResourceException e) {
			log.error("Failed to save User Profile.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<UserProfileDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<UserProfileDto>> update(@PathVariable("userId") String userId,
			@RequestBody @Valid UpdateUserProfileDto updateUserProfileDto) {
		UserProfileDto updatedUserProfileDto;
		try {
			updatedUserProfileDto = userProfileService.update(userId, updateUserProfileDto);
		} catch (ResourceException e) {
			log.error("Failed to save User Profile.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<UserProfileDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
		return ResponseEntity.status(HttpStatus.OK).body(Response.<UserProfileDto>builder()
				.status(HttpStatus.OK.value()).message("success").body(updatedUserProfileDto).build());
	}

	@DeleteMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<UserProfileDto>> update(@PathVariable("userId") String userId) {
		try {
			userProfileService.deleteUser(userId);
		} catch (ResourceException e) {
			log.error("Failed to delete User Profile.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<UserProfileDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(
				Response.<UserProfileDto>builder().status(HttpStatus.ACCEPTED.value()).message("accepted").build());
	}

	@PutMapping(value = "/picture", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<String>> updatePicture(
			@RequestParam(value = "file", required = false) MultipartFile mediaFile) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			if (mediaFile == null) {
				throw new ResourceException(ErrorCodes.RESOURCE_VALIDATION_FAILED, "file is required.");
			}
			FileObjectDto fileObjectDto = new FileObjectDto();
			fileObjectDto.setOriginalFilename(mediaFile.getOriginalFilename());
			fileObjectDto.setContent(mediaFile.getBytes());
			fileObjectDto.setContentType(mediaFile.getContentType());
			String profilePictureUtl = userProfileService.updateProfilePicture(currentUser.getUserId(), fileObjectDto);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<String>builder().status(HttpStatus.OK.value())
					.message("success").body(profilePictureUtl).build());
		} catch (ResourceException e) {
			log.error("Failed to save User Profile picture.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<String>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		} catch (IOException e) {
			log.error("Failed to save User Profile picture", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.<String>builder()
					.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).message(e.getMessage()).build());
		}
	}

	@PostMapping("/refresh")
	public ResponseEntity<Response<LoginResponseDto>> refresh() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			String jwtToken = jwtService.generateToken(currentUser);

			UserProfileDto userProfileDto = userProfileService.getUserProfileByUsername(currentUser.getUsername());

			LoginResponseDto loginResponse = new LoginResponseDto();
			loginResponse.setJwtToken(jwtToken);
			loginResponse.setRefreshToken(jwtService.generateRefreshToken(currentUser));
			loginResponse.setExpiresIn(jwtService.getExpirationTime());
			loginResponse.setUserProfile(userProfileDto);
			return ResponseEntity
					.ok(Response.<LoginResponseDto>builder().status(HttpStatus.OK.value()).body(loginResponse).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<LoginResponseDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PostMapping("/login")
	public ResponseEntity<ServiceResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
		ServiceResponse response = userService.login(loginUserDto);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@PostMapping("/password-change")
	public ResponseEntity<ServiceResponse> changePassword(@RequestBody ChangePasswordDto passwordDto) {

		// Authentication authentication =
		// SecurityContextHolder.getContext().getAuthentication();
		// UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		ServiceResponse response = userService.changePassword(passwordDto, null);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	@GetMapping("/forgot-password")
	public ResponseEntity<ServiceResponse> forgotPassword(@RequestParam String identifier,
			@RequestParam(required = false, defaultValue = "false") boolean forWeb, HttpServletRequest request) {
		// Authentication authentication =
		// SecurityContextHolder.getContext().getAuthentication();
		// UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		String domainUrl = request.getHeader("origin");

		ServiceResponse response = forWeb ? userService.forgotPasswordForWeb(identifier, null, domainUrl)
				: userService.forgotPasswordForApp(identifier, null);

		return new ResponseEntity<>(response, response.getHttpStatus());
	}

	// for web login exist
	@GetMapping(value = "/otp/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> sendOtp(@PathVariable("identifier") String identifier,
			@RequestParam(value = "sendotp", defaultValue = "false") Boolean sendOtp,
			@RequestParam(value = "password", defaultValue = "false") Boolean usePassword, HttpServletRequest request) {
		try {
			String domainUrl = request.getHeader("origin");

			ServiceResponse serviceResponse = userService.checkUserExistsAndSendOtp(identifier, sendOtp, usePassword,
					domainUrl);
			return new ResponseEntity<>(serviceResponse, serviceResponse.getHttpStatus());
		} catch (ResourceException e) {
			log.error("Failed to save User Profile.", e);
			UserExistsDto userExistsDto = new UserExistsDto();
			userExistsDto.setUserExists(false);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).body(userExistsDto).build());
		}
	}

	// This end-point is used by flutter team for sending OTP
	@GetMapping("/exists/{identifier}")
	public ResponseEntity<?> exists(@PathVariable("identifier") String identifier,
			@RequestParam(value = "sendotp", defaultValue = "false") Boolean sendOtp,
			@RequestParam(value = "password", defaultValue = "false") Boolean usePassword) {
		try {
			log.info("usePassword before: {}", usePassword.booleanValue());
			// Discussed with Sanjay on 20th June 2025
			// Setting as false as tournament is on 21st and apk approval takes time
			// TODO: remove this once apk fix the issue and the latest app is approved
			usePassword = Boolean.FALSE;
			log.info("usePassword after: {}", usePassword.booleanValue());
			ServiceResponse serviceResponse = userService.checkUserExistsAndSendOtp(identifier, sendOtp, usePassword,
					null);
			return ResponseEntity.ok(Response.builder().status(HttpStatus.OK.value()).message("User exists")
					.body(serviceResponse.getBody()).build());
		} catch (ResourceException e) {
			UserExistsDto userExistsDto = new UserExistsDto();
			userExistsDto.setUserExists(false);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<UserExistsDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).body(userExistsDto).build());
		}
	}

	// @GetMapping("/exists/{phoneNumber}")
	// public ResponseEntity<Response<UserExistsDto>>
	// exists(@PathVariable("phoneNumber") String username,
	// @RequestParam(value = "sendotp", defaultValue = "false") Boolean sendOtp,
	// @RequestParam(value = "password", defaultValue = "false") Boolean
	// usePassword) throws ResourceException {
	// try {
	// userProfileService.exists(username);
	//
	// boolean otpByPass = this.otpByPass
	// && (CollectionUtils.isNullOrEmpty(otpByPassUsernames) ||
	// otpByPassUsernames.contains(username));
	// String otp = null;
	// if (sendOtp && !otpByPass) {
	// otp = userProfileService.sendOtp(username);
	// }
	// UserExistsDto userExistsDto = new UserExistsDto();
	// userExistsDto.setUserExists(true);
	// userExistsDto.setOtp(otp);
	// return
	// ResponseEntity.ok(Response.<UserExistsDto>builder().status(HttpStatus.OK.value())
	// .message("User exists").body(userExistsDto).build());
	// } catch (ResourceException e) {
	// UserExistsDto userExistsDto = new UserExistsDto();
	// userExistsDto.setUserExists(false);
	// return
	// ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<UserExistsDto>builder()
	// .status(e.getErrorCodes().getCustomError()).message(e.getMessage()).body(userExistsDto).build());
	// }
	// }

	@GetMapping("/phone/{phoneNumber}")
	public ResponseEntity<Response<List<UserProfileDto>>> getUserProfileByPhonenumber(
			@PathVariable("phoneNumber") String phoneNumber) throws ResourceException {
		try {

			return ResponseEntity
					.ok(Response.<List<UserProfileDto>>builder().status(HttpStatus.OK.value()).message("success")
							.body(userProfileService.getUserProfileDtoListByUsername(phoneNumber)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<UserProfileDto>>builder()
							.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("/search")
	public ResponseEntity<Response<List<UserProfileMinDto>>> searchUsers(@RequestParam("t") String searchTxt)
			throws ResourceException {
		try {
			return ResponseEntity.ok(Response.<List<UserProfileMinDto>>builder().status(HttpStatus.OK.value())
					.message("success").body(userProfileService.searchUser(searchTxt)).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<List<UserProfileMinDto>>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping("/notifications")
	public ResponseEntity<Response<List<NotificationDto>>> getNotifications() throws ResourceException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();
		return ResponseEntity.ok(Response.<List<NotificationDto>>builder().status(HttpStatus.OK.value())
				.message("success").body(notificationService.getNotifications(currentUser.getUserId())).build());
	}

	@PostMapping(value = "/notification/{phoneNumber}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<UserProfileDto>> testNotification(@PathVariable("phoneNumber") String phoneNumber,
			@Valid @RequestBody TestNotificationDto testNotificationDto) throws ResourceException {
		try {
			UserProfileDto userProfileDto = userProfileService.getUserProfileByUsername(phoneNumber);
			notificationService.testNotification(userProfileDto, testNotificationDto);
			return ResponseEntity
					.ok(Response.<UserProfileDto>builder().status(HttpStatus.OK.value()).message("success").build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<UserProfileDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

	@GetMapping("/phone/exists/{phoneNumber}")
	public ResponseEntity<Response<List<UserProfileDetails>>> getUserExistsByPhonenumber(
			@PathVariable("phoneNumber") String phoneNumber, HttpServletRequest request) throws ResourceException {

		String academyDomain = request.getHeader("origin");

		List<UserProfileDetails> details = userProfileService.getUsersByUsername(phoneNumber, academyDomain);

		return ResponseEntity.ok(Response.<List<UserProfileDetails>>builder().status(HttpStatus.OK.value())
				.message(!details.isEmpty() ? "success" : "User not found").body(details).build());
	}

	@PostMapping("switch-user")
	public ResponseEntity<Response<LoginResponseDto>> switchProfile(
			@RequestParam(name = "to", required = true) String userId) throws ResourceException {
		log.info("userId: {}", userId);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		log.info("currentUser id: {}", currentUser.getUserId());
		LoginResponseDto details = userProfileService.switchProfile(currentUser, userId);

		return ResponseEntity.ok(Response.<LoginResponseDto>builder().status(HttpStatus.OK.value()).message("success")
				.body(details).build());
	}

	@PostMapping("make-primary")
	public ResponseEntity<Response<List<UserProfileDto>>> switchPrimaryProfile(
			@RequestParam(name = "to", required = true) String userId) throws ResourceException {
		log.info("userId: {}", userId);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		List<UserProfileDto> details = userProfileService.switchPrimaryProfile(currentUser, userId);

		return ResponseEntity.ok(Response.<List<UserProfileDto>>builder().status(HttpStatus.OK.value())
				.message("success").body(details).build());
	}

	@GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<UserStatsDto>> getUserStats(
			@RequestParam(value = "userId", required = false) String userId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			UserStatsDto userStatsDto = userProfileService
					.getUserStats(StringUtils.isNotEmpty(userId) ? userId : currentUser.getUserId());
			return ResponseEntity.status(HttpStatus.OK).body(Response.<UserStatsDto>builder()
					.status(HttpStatus.OK.value()).message("success").body(userStatsDto).build());
		} catch (ResourceException e) {
			log.error("Failed to get User Stats.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<UserStatsDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@PostMapping(value = "/stats/all", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<?>> getUserStats(
			@RequestBody List<String> userId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();

			List<String> userIds = CollectionUtils.isEmpty(userId)
					? new ArrayList<>(Collections.singleton(currentUser.getUserId()))
					: userId;

			Map<String, UserStatsDto> userStatsDto = userProfileService.getUsersStats(userIds);
			return ResponseEntity.status(HttpStatus.OK).body(Response.<Map<String, UserStatsDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(userStatsDto).build());
		} catch (ResourceException e) {
			log.error("Failed to get User Stats.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<UserStatsDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

	@GetMapping("matches")
	public ResponseEntity<Response<?>> getUserMatches(@RequestParam(value = "userId", required = false) String userId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDetail currentUser = (UserDetail) authentication.getPrincipal();
			List<BadmintonMatchDto> userMatches = userProfileService
					.getUserMatches(StringUtils.isNotEmpty(userId) ? userId : currentUser.getUserId());
			return ResponseEntity.status(HttpStatus.OK).body(Response.<List<BadmintonMatchDto>>builder()
					.status(HttpStatus.OK.value()).message("success").body(userMatches).build());
		} catch (ResourceException e) {
			log.error("Failed to get User Stats.", e);
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode())
					.body(Response.<UserStatsDto>builder().status(e.getErrorCodes().getCustomError())
							.message(e.getMessage()).build());
		}
	}

}
