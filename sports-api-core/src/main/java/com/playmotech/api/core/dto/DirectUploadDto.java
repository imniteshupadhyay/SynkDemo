package com.playmotech.api.core.dto;

import java.util.Date;

import lombok.Data;

@Data
public class DirectUploadDto {
    // Request info
    private String fileName;
    private String contentType;
    private Long fileSize;

    // User for tracking uploads
    private String userId;
    private String moduleType; // VIDEO_ANALYZER, POST, etc.
    private String moduleId; // ID of related entity

    // Response info
    private String fileKey;
    private String presignedUrl;
    private Date expiresAt;
}
