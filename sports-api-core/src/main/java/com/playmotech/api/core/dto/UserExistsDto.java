package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class UserExistsDto {
	private boolean userExists;
	private String otp;
	private boolean usePassword;
	private boolean firstLogin;
	private boolean verified;
}
