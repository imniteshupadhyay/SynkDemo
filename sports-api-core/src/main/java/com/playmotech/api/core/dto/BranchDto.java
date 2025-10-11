package com.playmotech.api.core.dto;

import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Created By: deep.patel
 **/

@Data
public class BranchDto {
	private String id;
	@NotEmpty(message = "Cannot be empty.")
	private String name;
	private String address;
	private String addressLine1;
	private String addressLine2;
	private String pincode;
	private String city;
	private String state;
	private String country;
	@NotEmpty(message = "Cannot be empty.")
	private String phoneNumber;
	private Set<String> coachUserIds;
	private int nets;
	private int courts;
	private int turf;
	private boolean gym;
	private int playArea;
	private boolean others;
	private List<String> facilityTypes;
}
