package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bulk_upload_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private BulkType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false)
	private OperationType operationType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "program_id", referencedColumnName = "id")
	private Course program;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private UploadStatus status;

	@Column(name = "error_report_url")
	private String errorReportUrl;

	@Column(name = "excel_file_url")
	private String excelFileUrl;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "total_records")
	private Integer totalRecords;

	@Column(name = "success_count")
	private Integer successCount;

	@Column(name = "failure_count")
	private Integer failureCount;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "remarks", columnDefinition = "TEXT")
	private String remarks;

	@JsonIgnore
	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@JsonIgnore
	@CreationTimestamp
	@Column(name = "inserted_on", updatable = false)
	private LocalDateTime insertedOn;

	@JsonIgnore
	@UpdateTimestamp
	@Column(name = "updated_on")
	private LocalDateTime updatedOn;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_id", referencedColumnName = "id", nullable = false)
	private UserProfile uploadedBy;

	public enum BulkType {
		COACHES, PLAYERS, PROGRAMS, PLAYER_ENROLLEMENTS
	}

	public enum OperationType {
		ADD, EDIT
	}

	public enum UploadStatus {
		IN_PROGRESS, COMPLETED, COMPLETED_WITH_ERRORS, FAILED
	}

	@Transient
	@JsonIgnore
	private List<BulkType> bulkTypes;

	@Transient
	@JsonIgnore
	private List<UploadStatus> uploadStatuses;

}
