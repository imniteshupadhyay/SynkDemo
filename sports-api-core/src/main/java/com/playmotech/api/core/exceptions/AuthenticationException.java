package com.playmotech.api.core.exceptions;

import com.playmotech.api.core.constants.ErrorCodes;

import lombok.Getter;

/**
 * Created By: deep.patel
 **/
public class AuthenticationException extends Exception {

	@Getter
	private final ErrorCodes errorCodes;

	@Getter
	private final String message;

	public AuthenticationException(ErrorCodes errorCodes, String message) {
		super();
		this.errorCodes = errorCodes;
		this.message = message;
	}
}
