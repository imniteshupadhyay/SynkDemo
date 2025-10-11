package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class LoginUserOtpDto {
	private String username;
	private String otp;
}
