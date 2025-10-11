package com.playmotech.api.core.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.playmotech.api.core.dto.EnquiryDto;
import com.playmotech.api.core.dto.EnquiryRequestDto;
import com.playmotech.api.core.dto.Response;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.services.IEnquiryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/enquiries")
public class EnquiryController extends BaseController {

	@Autowired
	private IEnquiryService enquiryService;

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Response<EnquiryDto>> createEnquiry(@Valid @RequestBody EnquiryRequestDto enquiryRequestDto) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		UserDetail currentUser = (UserDetail) authentication.getPrincipal();

		try {
			EnquiryDto createdEnquiry = enquiryService.createEnquiry(enquiryRequestDto, currentUser);
			return ResponseEntity.status(HttpStatus.OK)
					.body(Response.<EnquiryDto>builder().status(HttpStatus.OK.value())
							.message("Enquiry created successfully").body(createdEnquiry).build());
		} catch (ResourceException e) {
			return ResponseEntity.status(e.getErrorCodes().getHttpStatusCode()).body(Response.<EnquiryDto>builder()
					.status(e.getErrorCodes().getCustomError()).message(e.getMessage()).build());
		}
	}

}
