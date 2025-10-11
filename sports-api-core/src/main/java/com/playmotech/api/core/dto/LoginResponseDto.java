package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class LoginResponseDto {
	private String jwtToken;
	private String refreshToken;
	private long expiresIn;
	private UserProfileDto userProfile;
}
