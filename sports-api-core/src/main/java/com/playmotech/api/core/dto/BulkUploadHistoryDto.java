package com.playmotech.api.core.dto;

import java.time.LocalDateTime;

import com.playmotech.api.core.dao_postgres.BulkUploadHistory.BulkType;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.UploadStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadHistoryDto {

    private Long id;
    private BulkType type;
    private UploadStatus status;
    private String errorReportUrl;
    private String excelFileUrl;
    private String fileName;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String remarks;
    private String academyId;
    private String programId;
    private String uploadedById;
}
