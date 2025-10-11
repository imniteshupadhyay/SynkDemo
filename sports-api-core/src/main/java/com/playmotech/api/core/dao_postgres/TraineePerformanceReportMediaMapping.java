package com.playmotech.api.core.dao_postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "trainee_performance_report_media_mappings")
@Data
public class TraineePerformanceReportMediaMapping {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "report_id", referencedColumnName = "id")
	private TraineePerformanceReport report;
	@Column(name = "media_url")
	private String mediaUrl;
}
