package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.playmotech.api.core.constants.ComparisonType;
import com.playmotech.api.core.constants.Sports;
import com.playmotech.api.core.constants.UnitType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "assessment_parameter_config")
public class AssessmentParameterConfig {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "parameter_id")
	private String parameterId;

	@Column(nullable = false, unique = true)
	private String parameterName;

	@Column(columnDefinition = "TEXT")
	private String parameterDescription;

	@Enumerated(EnumType.STRING)
	@Column(name = "sport")
	private Sports sport;

	@Enumerated(EnumType.STRING)
	@Column(name = "parameter_type", nullable = false)
	private ParameterType parameterType;

	@Enumerated(EnumType.STRING)
	@Column(name = "unit_type", nullable = false)
	private UnitType unitType;

	@Enumerated(EnumType.STRING)
	@Column(name = "comparison_type", nullable = false)
	private ComparisonType comparisonType;

	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false, updatable = false)
	private Timestamp createdOn;

	@UpdateTimestamp
	@Column(name = "updated_on", insertable = false)
	private Timestamp updatedOn;

	/**
	 * Enum representing different types of parameters.
	 */
	public enum ParameterType {
		TIME, // time-based (uses SEC, MIN, HOUR)
		DISTANCE, // distance-based (uses M, CM, KM)
		WEIGHT, // weight-based (uses KG, G, LB)
		SPEED, // speed-based (uses MPS, KPH, MPH)
		COUNT, // repetitions, attempts
		SCORE, // points or scoring system
		PERCENTAGE // percentage values
	}
}
