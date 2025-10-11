package com.playmotech.api.core.services;

import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.response.ServiceResponse;

public interface BulkUploadUserService {

	ServiceResponse bulkUploadProgramsFromExcel(MultipartFile file, String academyId, String userId);

	ServiceResponse bulkUploadProgramsCoachesFromExcel(MultipartFile file, String academyId, String userId);

	ServiceResponse bulkUploadProgramsPlayersFromExcel(MultipartFile file, String academyId, String userId);

	ServiceResponse bulkUploadProgramsMapPlayersFromExcel(MultipartFile file, String academyId, String programId,
			String userId);

	ServiceResponse bulkUploadProgramsPayments(String academyId, String programId, String userId);

}
