package com.playmotech.api.core.dto;

import lombok.Data;

@Data
public class ChangePasswordDto {
	private String email;
	private String oldPassword;
	private String newPassword;

}
