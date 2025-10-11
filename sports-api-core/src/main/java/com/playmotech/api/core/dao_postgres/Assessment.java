package com.playmotech.api.core.dao_postgres;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.AssessmentStatus;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "assessment")
public class Assessment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private String id;

	@Column(name = "forAllAcademies", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean forAllAcademies;

	@Column(name = "assessment_title", nullable = false)
	private String assessmentTitle;

	@Column(name = "assessment_description", columnDefinition = "TEXT")
	private String assessmentDescription;

	@Column(name = "start_date", nullable = false)
	private Date startDate;

	@Column(name = "end_date", nullable = false)
	private Date endDate;

	@Column(name = "location")
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(name = "sport")
	private Sports sport;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<AssessmentParameter> parameters;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<AssessmentAcademyMapping> academyMappings;

	@Enumerated(EnumType.STRING)
	@Column(name = "assessment_status", nullable = false)
	private AssessmentStatus assessmentStatus;

	@Column(name = "registration_fee")
	private Double registrationFee;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false)
	private Timestamp createdOn;

	@UpdateTimestamp
	@Column(name = "updated_on")
	private Timestamp updatedOn;

	@Transient
	@JsonIgnore
	private List<String> status;
}
