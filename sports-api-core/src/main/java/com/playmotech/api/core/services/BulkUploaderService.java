package com.playmotech.api.core.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.excel.ParsedRow;

public interface BulkUploaderService {

	ServiceResponse bulkUploadCoachesFromExcel(MultipartFile file, String academyId, boolean save, boolean edit);

	ServiceResponse bulkUploadCoachesFromExcel(List<ParsedRow> rows, String academyId, boolean save, String userId,
			boolean edit);

	ServiceResponse bulkUploadPlayersFromExcel(MultipartFile file, String academyId, boolean save, boolean edit);

	ServiceResponse bulkUploadPlayersFromExcel(List<ParsedRow> rows, String academyId, boolean save, String userId,
			boolean edit);

	ServiceResponse bulkUploadProgramsMapPlayersFromExcel(MultipartFile file, String academyId, String programId,
			String userId, boolean save);

	ServiceResponse bulkUploadProgramsMapPlayersFromExcel(List<ParsedRow> rows, String academyId, String programId,
			String userId, boolean save);

	ServiceResponse bulkUploadProgramsFromExcel(MultipartFile file, String academyId, String userId, boolean save);

	ServiceResponse bulkUploadProgramsFromExcel(List<ParsedRow> rows, String academyId, String userId, boolean save);

}
