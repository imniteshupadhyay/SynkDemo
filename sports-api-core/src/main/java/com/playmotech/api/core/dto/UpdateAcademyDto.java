package com.playmotech.api.core.dto;

import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class UpdateAcademyDto {
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
}
