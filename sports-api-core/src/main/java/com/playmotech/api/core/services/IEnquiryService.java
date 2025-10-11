package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.dto.EnquiryDto;
import com.playmotech.api.core.dto.EnquiryRequestDto;
import com.playmotech.api.core.dto.UserDetail;
import com.playmotech.api.core.exceptions.ResourceException;

public interface IEnquiryService {
	EnquiryDto createEnquiry(EnquiryRequestDto enquiryRequestDto, UserDetail userDetail) throws ResourceException;

	EnquiryDto updateEnquiry(String academyId, String enquiryId, EnquiryRequestDto enquiryRequestDto,
			UserDetail userDetail) throws ResourceException;

	List<EnquiryDto> getAllEnquiriesForUser(String academyId, String userId, String status) throws ResourceException;

	void deleteEnquiry(String academyId, String enquiryId, UserDetail userDetail) throws ResourceException;
}
