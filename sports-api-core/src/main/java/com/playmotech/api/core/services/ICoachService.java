package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AddCoachToAcademyRequestDto;
import com.playmotech.api.core.dto.CoachDetailsDto;
import com.playmotech.api.core.dto.CoachPerformanceReportDto;
import com.playmotech.api.core.dto.SubmitCoachPerformanceReportRequestDto;
import com.playmotech.api.core.dto.ToggleCoachStatusDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;

public interface ICoachService {
	void addCoachToAcademy(List<AddCoachToAcademyRequestDto> coaches, String academyId) throws ResourceException;

	void updateCoachInAcademy(List<AddCoachToAcademyRequestDto> coaches, String academyId) throws ResourceException;

	List<CoachDetailsDto> getCoachesByAcademyIdAndNameAndPhoneNumber(String academyId, String name, String phoneNumber,
			String searchTxt, boolean coach) throws ResourceException;

	void removeCoachesFromAcademy(List<String> coaches, String academyId) throws ResourceException;

	void toggleCoachesStatusInAcademy(String academyId, ToggleCoachStatusDto toggleCoachStatusDto)
			throws ResourceException;

	List<AcademyDto> getAcademiesByCoachUserId(String coachUserId) throws ResourceException;

	CoachPerformanceReportDto submitCoachPerformanceReport(String coachUserId, String traineeUserId,
			SubmitCoachPerformanceReportRequestDto coachPerformanceReportRequestDto) throws ResourceException;

	ServiceResponse getList(String userId, List<String> academyIds, List<String> courseIds, List<String> sports,
			String academyDomain, boolean export);
}
