package com.playmotech.api.core.response.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceTrendDao {

	private String periodLabel;
	private Long weekNumber;
	private Long totalStudents;
	private Long presentStudents;
	private Long absentStudents;

}
