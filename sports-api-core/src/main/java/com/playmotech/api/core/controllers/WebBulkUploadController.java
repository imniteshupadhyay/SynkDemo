package com.playmotech.api.core.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.excel.ParsedRow;
import com.playmotech.api.core.services.BulkUploadUserService;
import com.playmotech.api.core.services.BulkUploaderService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping("web/bulk")
@RequiredArgsConstructor
public class WebBulkUploadController {

	private final BulkUploadUserService bulkUploadService;

	private final BulkUploaderService uploaderService;

	@PostMapping(value = "/bulk-upload/programs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ServiceResponse> bulkUploadPrograms(@RequestParam("userId") String userId,
			@RequestParam("academyId") String academyId, @RequestParam("file") MultipartFile file) {
		ServiceResponse response = bulkUploadService.bulkUploadProgramsFromExcel(file, academyId, userId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "/bulk-upload/programs-coaches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ServiceResponse> bulkUploadProgramsAndCoaches(@RequestParam("userId") String userId,
			@RequestParam("academyId") String academyId, @RequestParam("file") MultipartFile file) {
		ServiceResponse response = bulkUploadService.bulkUploadProgramsCoachesFromExcel(file, academyId, userId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "/bulk-upload/programs-players", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ServiceResponse> bulkUploadProgramsAndPLayers(@RequestParam("userId") String userId,
			@RequestParam("academyId") String academyId, @RequestParam("file") MultipartFile file) {
		ServiceResponse response = bulkUploadService.bulkUploadProgramsPlayersFromExcel(file, academyId, userId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "/bulk-upload/programs-map", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ServiceResponse> bulkUploadProgramsMapPlayers(@RequestParam("userId") String userId,
			@RequestParam("academyId") String academyId, @RequestParam("programId") String programId,
			@RequestParam("file") MultipartFile file) {
		ServiceResponse response = bulkUploadService.bulkUploadProgramsMapPlayersFromExcel(file, academyId, programId,
				userId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "/bulk-upload/payments")
	public ResponseEntity<ServiceResponse> bulkUploadProgramsMapPlayers(@RequestParam("userId") String userId,
			@RequestParam("academyId") String academyId, @RequestParam("programId") String programId) {
		ServiceResponse response = bulkUploadService.bulkUploadProgramsPayments(academyId, programId, userId);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ServiceResponse> bulkUpload(@RequestParam("file") MultipartFile file,
			@RequestParam("type") String type, @RequestParam(required = false) String academyId,
			@RequestParam(required = false) String userId, @RequestParam(required = false) String programId,
			@RequestParam(defaultValue = "false") boolean save,
			@RequestParam(name = "edit", defaultValue = "false") boolean edit) {

		ServiceResponse response = null;

		if ("coaches".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadCoachesFromExcel(file, academyId, save, edit);
		} else if ("players".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadPlayersFromExcel(file, academyId, save, edit);
		} else if ("programs".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadProgramsFromExcel(file, academyId, userId, save);
		} else if ("enrollments".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadProgramsMapPlayersFromExcel(file, academyId, programId, userId, save);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@PostMapping(value = "upload-save")
	public ResponseEntity<ServiceResponse> bulkUploadAsync(@RequestBody List<ParsedRow> rows,
			@RequestParam("type") String type, @RequestParam(required = false) String academyId,
			@RequestParam(required = false) String userId, @RequestParam(required = false) String programId,
			@RequestParam(name = "edit", defaultValue = "false") boolean edit) {

		ServiceResponse response = null;

		if ("coaches".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadCoachesFromExcel(rows, academyId, true, userId, edit);
		} else if ("players".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadPlayersFromExcel(rows, academyId, true, userId, edit);
		} else if ("programs".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadProgramsFromExcel(rows, academyId, userId, true);
		} else if ("enrollments".equalsIgnoreCase(type)) {
			response = uploaderService.bulkUploadProgramsMapPlayersFromExcel(rows, academyId, programId, userId, true);
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

}
