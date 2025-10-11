package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "video_analytics")
@SQLRestriction("deleted = false")   // always skip soft-deleted rows
public class VideoAnalytics {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "video_id", nullable = false)
	private VideoAnalyzer video;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false, updatable = false)
	private Timestamp createdOn;

	@Column(name = "created_by")
	private String createdBy;

	@UpdateTimestamp
	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@Column(name = "updated_by")
	private String updatedBy;

	@Column(name = "analysis_json", columnDefinition = "TEXT")
	private String analysisJson;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private AnalysisStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "sports_type", nullable = false)
	private SportsType sportsType;

	@Column(name = "extra_params", columnDefinition = "TEXT")
	private String extraParams;

	@Enumerated(EnumType.STRING)
	@Column(name = "analysis_source")
	private AnalysisSource analysisSource;

	@Column(name = "additional_context", columnDefinition = "TEXT")
	private String additionalContext;

	@JsonIgnore
	@Column(name = "deleted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean deleted;

	// ========================= ENUMS =========================

	public enum AnalysisStatus {
		PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
	}

	public enum SportsType {
		CRICKET, BASKETBALL, FOOTBALL, TENNIS, BASEBALL
	}

	public enum AnalysisSource {
		SELF, GEMINI
	}
}
