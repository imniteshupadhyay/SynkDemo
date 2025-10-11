package com.playmotech.api.core.exceptions;

import com.playmotech.api.core.constants.ErrorCodes;

import lombok.Getter;

/**
 * Created By: deep.patel
 **/
public class ResourceException extends Exception {

	@Getter
	private final ErrorCodes errorCodes;

	@Getter
	private final String message;

	public ResourceException(ErrorCodes errorCodes, String message) {
		super();
		this.errorCodes = errorCodes;
		this.message = message;
	}
}
