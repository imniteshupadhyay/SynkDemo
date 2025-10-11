package com.playmotech.api.core.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.services.OrgConfigService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("organisation/config")
public class OrgConfigController {

	private final OrgConfigService orgConfigService;

	/**
	 * Update organisation-level checkPendingDue flag (true/false)
	 */
	@PatchMapping("check-pending-due")
	public ResponseEntity<ServiceResponse> setCheckPendingDue(@RequestParam("status") boolean status) {
		ServiceResponse response = orgConfigService.setCheckPendingDue(status);
		return new ResponseEntity<>(response, response.getHttpStatus());
	}
}
