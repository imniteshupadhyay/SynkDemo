package com.playmotech.api.core.dto;

import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserListBasedOnRoleDto {

	String id;
	String name;
	String phoneNumber;
	String emailId;
	String roleName;
	String sport;
	String branch;
	String titles;
	boolean inactive;
	List<String> associatedPrograms;
	List<String> sports;
	String academyIds;
	String status;
	String academyJson;
	private Timestamp createdOn;

}
