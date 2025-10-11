package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.playmotech.api.core.dao_postgres.VideoAnalytics.SportsType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prompt_templates")
public class PromptTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "sports_type", nullable = false)
	private SportsType sportsType;

	@Column(name = "template", columnDefinition = "TEXT", nullable = false)
	private String template;

	@Column(name = "deleted", nullable = false)
	private Boolean deleted;

	@Column(name = "description")
	private String description;

	@Column(name = "version")
	private Integer version;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false, updatable = false)
	private Timestamp createdOn;

	@UpdateTimestamp
	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "updated_by")
	private String updatedBy;
}