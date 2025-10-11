package com.playmotech.api.core.response.dao;

import java.time.LocalDateTime;

import com.playmotech.api.core.dao_postgres.BulkUploadHistory.BulkType;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.OperationType;
import com.playmotech.api.core.dao_postgres.BulkUploadHistory.UploadStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadHistoryDao {

    private Long id;
    private BulkType type;
	private OperationType operationType;
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
    private AcademyDao academy;
    private CourseDao program;
    private UserProfileDao uploadedBy;
}
