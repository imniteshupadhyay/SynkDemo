package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "assessment_academy_mapping")
public class AssessmentAcademyMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "mapping_id")
	private String mappingId;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assessment_id", referencedColumnName = "id", nullable = false)
	private Assessment assessment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id", nullable = false)
	private Academy academy;

	@CreationTimestamp
	@Column(name = "mapped_on", nullable = false, updatable = false)
	private Timestamp mappedOn;

}
