package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Status;

import lombok.Data;

@Data
public class CoachAcademyDetails {

	private String id;
	private String academyId;
	private String designation;
	private Status status;

}
