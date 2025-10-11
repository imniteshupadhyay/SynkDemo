package com.playmotech.api.core.response.dao;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Access Object for Certificate entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDao {
    
    private Long id;
    private String title;
    private String description;
    private Timestamp createdOn;
    private Timestamp updatedOn;
    private Timestamp issuedOn;
    private String fullName;
    private String eventName;
    private String certificateType;
    private Long rank;
    private String certificateUrl;
    private Boolean inactive;
    
    // References
    private String academyId;
    private String academyName;
    private String programId;
    private String programName;
    private String userId;
    private String createdById;
    private String createdByName;
}
