package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.constants.Sports;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class AcademyDto {
	private String id;

	@NotEmpty(message = "Cannot be empty.")
	private String internalId;

	@Size(min = 2, max = 254, message = "Name must be between 2 and 254 characters.")
	private String name;

	private String managerUserId;
	private List<BranchDto> branches;
	private List<GeoFenceDto> geoFences;

	@NotEmpty(message = "Cannot be empty.")
	private String emailId;

	@NotEmpty(message = "Cannot be empty.")
	private String phoneNumber;

	private String headquarter;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;

	private List<Sports> sports;
	private String startTime;
	private String endTime;
	private String iconUrl;
	private Integer roleId;
	private Map<String, String> config;
	private String orgId;

	/**
	 * Utility method to trim all string fields in this DTO.
	 */
	public void trimFields() {
		if (id != null)
			id = id.trim();
		if (internalId != null)
			internalId = internalId.trim();
		if (name != null)
			name = name.trim();
		if (managerUserId != null)
			managerUserId = managerUserId.trim();
		if (emailId != null)
			emailId = emailId.trim();
		if (phoneNumber != null)
			phoneNumber = phoneNumber.trim();
		if (headquarter != null)
			headquarter = headquarter.trim();
		if (addressLine1 != null)
			addressLine1 = addressLine1.trim();
		if (addressLine2 != null)
			addressLine2 = addressLine2.trim();
		if (pincode != null)
			pincode = pincode.trim();
		if (city != null)
			city = city.trim();
		if (state != null)
			state = state.trim();
		if (country != null)
			country = country.trim();
		if (startTime != null)
			startTime = startTime.trim();
		if (endTime != null)
			endTime = endTime.trim();
		if (iconUrl != null)
			iconUrl = iconUrl.trim();
	}
}
