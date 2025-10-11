package com.playmotech.api.core.services;

import com.playmotech.api.core.dto.FileObjectDto;
import com.playmotech.api.core.dto.VideoAnalyzerDto;
import com.playmotech.api.core.exceptions.ResourceException;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.utils.GenericFilter;

public interface VideoAnalyzerService {

    ServiceResponse getVideoAnalyzer(String id);

    ServiceResponse addVideoAnalyzer(VideoAnalyzerDto analyzerDto);

    ServiceResponse updateVideoAnalyzer(VideoAnalyzerDto analyzerDto, String userId);

    ServiceResponse deleteVideoAnalyzer(String id, String userId);

    ServiceResponse shareVideoAnalyzer(String id, String courseId, String playerId, String userId);

    ServiceResponse fileUpload(String userId, FileObjectDto fileObjectDto, FileObjectDto thumbnailFileObjectDto);

    ServiceResponse getVideoAnalyzersList(GenericFilter filter, String domainUrl);

    ServiceResponse generateDirectUploadUrl(String userId, String fileName);

    ServiceResponse confirmDirectUpload(String fileKey, String id) throws ResourceException;

}
