package com.playmotech.api.core.dao_postgres;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playmotech.api.core.constants.AgeCategory;

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

@Table(name = "trial_feedback")
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrialFeedback {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private String id;

	@Column(name = "technical_skill")
	private Integer technicalSkill;

	@Column(name = "fitness")
	private Integer fitness;

	@Column(name = "behavioural_skill")
	private Integer behaviouralSkill;

	@Enumerated(EnumType.STRING)
	@Column(name = "performance_level")
	private PerformanceLevel performanceLevel;

	@Enumerated(EnumType.STRING)
	@Column(name = "age_group")
	private AgeCategory ageGroup;

	@Column(name = "note", length = 1000)
	private String note;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trial_id", referencedColumnName = "id")
	@JsonBackReference
	private Trial trial;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;

	@JsonIgnore
	@CreationTimestamp
	@Column(name = "inserted_on", updatable = false)
	private LocalDateTime insertedOn;

	public enum PerformanceLevel {
		BEGINNER, INTERMEDIATE, PRO, EXPERT
	}
}