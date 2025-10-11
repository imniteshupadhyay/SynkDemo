package com.playmotech.api.core.response.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentAcademyMappingDao {
	private String mappingId;
	private String academyId;
	private String academyName;
}
