package com.playmotech.api.core.dto;

import java.util.List;

import lombok.Data;

@Data
public class CertificateRequest {

    private String title;
    private String description;
    private String certificateType;
    private Long rank;
    private String eventName;
    private String academyId;
    private String programId;
    private String createdById;

    // ✅ Users as key-value pairs
    private List<UserCertificateDto> users;

}
