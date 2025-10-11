package com.playmotech.api.core.services;

import java.util.List;
import java.util.Map;

import com.playmotech.api.core.dto.AcademyDto;
import com.playmotech.api.core.dto.AcademyLeadDto;
import com.playmotech.api.core.dto.BranchDto;
import com.playmotech.api.core.dto.CreateAcademyDto;
import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.UpdateAcademyDto;
import com.playmotech.api.core.exceptions.ResourceException;

import jakarta.servlet.http.HttpServletRequest;

public interface IAcademyService {
	boolean addLead(AcademyLeadDto academyLeadDto);

	AcademyDto registerAcademy(AcademyDto academyDto) throws ResourceException;

	AcademyDto updateAcademy(String academyId, String userId, UpdateAcademyDto updateAcademyDto)
			throws ResourceException;

	String updateAcademyPicture(String academyId, String userId, FileObjectDto fileObjectDto) throws ResourceException;

	AcademyDto registerAcademy(CreateAcademyDto createAcademyDto, Boolean sendOtp) throws ResourceException;

	AcademyDto getAcademyById(String academyId) throws ResourceException;

	List<AcademyDto> getAcademyByIds(List<String> academyIds);

	List<AcademyDto> getAcademyByManagerUserId(String managerUserId);

	AcademyDto updateAcademy(AcademyDto academyDto) throws ResourceException;

	boolean deleteAcademy(String academyId) throws ResourceException;

	BranchDto addBranchToAcademy(String academyId, BranchDto branchDto) throws ResourceException;

	BranchDto updateBranchOfAcademy(String academyId, String branchId, BranchDto branchDto) throws ResourceException;

	boolean deleteBranchFromAcademy(String academyId, String branchId) throws ResourceException;

	Map<String, String> getOrgConfigByDomainUrl(HttpServletRequest request) throws ResourceException;

	List<AcademyDto> getAcademiesByOrgId(String orgId);

	List<AcademyDto> getAcademiesByAppPackageName(String appPackageName);
}
