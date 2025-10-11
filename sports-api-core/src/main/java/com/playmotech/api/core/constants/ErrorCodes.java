package com.playmotech.api.core.constants;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Created By: deep.patel
 **/
public enum ErrorCodes {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST.value(), 4000), RESOURCE_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), 4001),
	RESOURCE_CONFLICT(HttpStatus.CONFLICT.value(), 4002),
	RESOURCE_CREATION_FAILED(HttpStatus.BAD_REQUEST.value(), 4003),
	UNABLE_TO_FETCH_USERID(HttpStatus.BAD_REQUEST.value(), 4004),
	RESOURCE_VALIDATION_FAILED(HttpStatus.BAD_REQUEST.value(), 4005),
	UPDATE_FIELDS_EMPTY(HttpStatus.BAD_REQUEST.value(), 4006),
	OLD_PASSWORD_NOT_MATCHED(HttpStatus.BAD_REQUEST.value(), 4007),
	USER_EXISTS_IN_ACADEMY(HttpStatus.BAD_REQUEST.value(), 4008),
	APP_LOGIN_NOT_AVAILABLE(HttpStatus.BAD_REQUEST.value(), 4021),
	USER_DOES_NOT_EXIST(HttpStatus.UNAUTHORIZED.value(), 4022), INVALID_PASSWORD(HttpStatus.UNAUTHORIZED.value(), 4023),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED.value(), 4024),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED.value(), 4025),
	COACH_USERID_DOESNT_EXIST(HttpStatus.BAD_REQUEST.value(), 4030),
	COACH_NOT_ASKED_FOR_ANALYSIS(HttpStatus.BAD_REQUEST.value(), 4031),
	VIDEO_ANALYSIS_SUBMITTED_ALREADY(HttpStatus.BAD_REQUEST.value(), 4032),
	INVALID_OTP(HttpStatus.BAD_REQUEST.value(), 4040), UNAUTHORIZED(HttpStatus.UNAUTHORIZED.value(), 4050),
	UNEXPECTED_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR.value(), 5000),
	MEDIA_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR.value(), 5001),
	USER_NOT_ASSOCIATED_WITH_ACADEMY(HttpStatus.BAD_REQUEST.value(), 5002),
	POST_NOT_FOUND(HttpStatus.BAD_REQUEST.value(), 5002), UNAUTHORIZED_ACTION(HttpStatus.UNAUTHORIZED.value(), 5003),
	NOT_FOUND(HttpStatus.NOT_FOUND.value(), 5004);

	@Getter
	int httpStatusCode;
	@Getter
	int customError;

	ErrorCodes(int httpStatusCode, int customError) {
		this.httpStatusCode = httpStatusCode;
		this.customError = customError;
	}
}
