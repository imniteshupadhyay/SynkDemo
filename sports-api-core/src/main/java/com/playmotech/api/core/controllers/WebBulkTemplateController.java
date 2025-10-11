package com.playmotech.api.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.BulkTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping("web/bulk/template")
@RequiredArgsConstructor
public class WebBulkTemplateController {

	private final BulkTemplateService bulkTemplateService;

	@GetMapping("coaches")
	public ResponseEntity<ServiceResponse> downloadCoachesTemplate(@RequestParam(required = false) String path,
			@RequestParam(required = false) String academyId,
			@RequestParam(name = "edit", defaultValue = "false") boolean edit) {
		ServiceResponse response = bulkTemplateService.downloadCoachTemplate(path, academyId, edit);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping("players")
	public ResponseEntity<ServiceResponse> downloadPlayerTemplate(@RequestParam(required = false) String path,
			@RequestParam(required = false) String academyId,
			@RequestParam(name = "edit", defaultValue = "false") boolean edit) {
		ServiceResponse response = bulkTemplateService.downloadPlayerTemplate(path, academyId, edit);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping("programs")
	public ResponseEntity<ServiceResponse> downloadProgramTemplate(@RequestParam String academyId,
			@RequestParam(required = false) String path) {
		ServiceResponse response = bulkTemplateService.downloadProgramTemplate(academyId, path);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping("programs-coaches")
	public ResponseEntity<ServiceResponse> downloadProgramCoachTemplate(@RequestParam String userId,
			@RequestParam String academyId, @RequestParam String path) {
		ServiceResponse response = bulkTemplateService.downloadProgramCoachTemplate(userId, academyId, path);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping("programs-player")
	public ResponseEntity<ServiceResponse> downloadProgramPlayerTemplate(@RequestParam String userId,
			@RequestParam String academyId, @RequestParam String path) {
		ServiceResponse response = bulkTemplateService.downloadProgramPlayerTemplate(userId, academyId, path);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

	@GetMapping("programs-enroll")
	public ResponseEntity<ServiceResponse> downloadProgramEnrollTemplate(@RequestParam String userId,
			@RequestParam String academyId, @RequestParam(required = false) String path) {
		ServiceResponse response = bulkTemplateService.downloadProgramEnrollmentTemplate(academyId, path);
		return ResponseEntity.status(response.getHttpStatus()).body(response);
	}

}
