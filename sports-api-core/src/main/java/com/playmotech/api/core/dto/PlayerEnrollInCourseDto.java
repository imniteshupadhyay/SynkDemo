package com.playmotech.api.core.dto;

import java.util.List;

import com.playmotech.api.core.constants.Status;

import lombok.Data;

@Data
public class PlayerEnrollInCourseDto {

	private String id;
	private String academyId;
	private Status academyStatus;

	private List<EnrollTraineeInCourseDto> enrollInPrograms;
}
