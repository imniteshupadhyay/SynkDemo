package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class LoginUserDto {

	private String username;
	private String email;
	private String phoneNumber;

	private String otp;
	private String password;

}
