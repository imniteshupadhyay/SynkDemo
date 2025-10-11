package com.playmotech.api.core.services;

import org.springframework.web.multipart.MultipartFile;

import com.playmotech.api.core.dto.CertificateRequest;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface CertificateService {

	ServiceResponse getPlayers(GenericFilter filter);

	ServiceResponse getCertificates(GenericFilter filter);

	ServiceResponse storeCertificates(CertificateRequest request, MultipartFile file);

}
