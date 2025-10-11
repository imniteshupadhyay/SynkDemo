package com.playmotech.api.core.response.dao;

import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Sports;

import lombok.Data;

@Data
public class ProgramDao {

	private String id;
	private String title;
	private Sports sport;
	private AgeCategory ageCategory;

}