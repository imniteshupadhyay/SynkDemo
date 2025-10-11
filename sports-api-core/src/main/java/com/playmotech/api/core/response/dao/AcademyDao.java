package com.playmotech.api.core.response.dao;

import java.util.List;

import lombok.Data;

@Data
public class AcademyDao {

	private String id;
	private String name;
	private List<ProgramDao> programs;

}
