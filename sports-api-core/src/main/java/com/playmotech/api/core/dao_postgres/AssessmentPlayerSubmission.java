package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.playmotech.api.core.constants.Gender;

import jakarta.persistence.CascadeType;
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
@Table(name = "assessment_player_submission")
public class AssessmentPlayerSubmission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "submission_id")
	private String submissionId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assessment_id", nullable = false)
	private Assessment assessment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registration_id", referencedColumnName = "registration_id", nullable = false)
	private AssessmentPlayerRegistration registration;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", referencedColumnName = "id", nullable = false)
	private UserProfile player;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender")
	private Gender gender;

	@Column(name = "submitted_on")
	private Timestamp submittedOn;

	@Enumerated(EnumType.STRING)
	@Column(name = "submission_status", nullable = false)
	private SubmissionStatus submissionStatus;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<AssessmentPlayerSubmissionParameter> submissionParameters;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false, updatable = false)
	private Timestamp createdOn;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@UpdateTimestamp
	@Column(name = "updated_on", insertable = false)
	private Timestamp updatedOn;

	@Column(name = "updated_by")
	private String updatedBy;

	public enum SubmissionStatus {
		SAVED, SUBMITTED
	}

	@Transient
	@JsonIgnore
	private List<String> status;

	@Transient
	@JsonIgnore
	private String assessmentId;
}
