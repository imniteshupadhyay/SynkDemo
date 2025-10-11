package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playmotech.api.core.constants.AgeCategory;
import com.playmotech.api.core.constants.Gender;
import com.playmotech.api.core.constants.LeadStatus;
import com.playmotech.api.core.constants.Sports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "leads")
public class Leads {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "name", columnDefinition = "text")
	private String name;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "email_id")
	private String emailId;

	@Column(name = "gender")
	@Enumerated(value = EnumType.STRING)
	private Gender gender;

	@Column(name = "sports")
	@Enumerated(value = EnumType.STRING)
	private Sports sports;

	@Column(name = "age_category")
	@Enumerated(value = EnumType.STRING)
	private AgeCategory ageCategory;

	@Column(name = "inactive", columnDefinition = "default false")
	private boolean inactive;

	@Column(name = "address_line_1", columnDefinition = "text")
	private String addressLine1;

	@Column(name = "address_line_2", columnDefinition = "text")
	private String addressLine2;

	@ManyToOne
	@JoinColumn(name = "lead_source_id", referencedColumnName = "id")
	private LeadSource leadSource;

	@Column(name = "trial_status")
	private String trialStatus;

	@Column(name = "dob")
	private LocalDate dob;

	@Column(name = "lead_source_reason", columnDefinition = "text")
	private String leadSourceReason;

	@Column(name = "lead_status")
	@Enumerated(value = EnumType.STRING)
	private LeadStatus leadStatus;

	@ManyToOne
	@JoinColumn(name = "assigned_academy", referencedColumnName = "id")
	@JsonIgnore
	private Academy assignedAcademy;

	@ManyToOne
	@JoinColumn(name = "assigned_coach", referencedColumnName = "id")
	@JsonIgnore
	private UserProfile assignedCoach;

	@Column(name = "created_on", updatable = false)
	@CreationTimestamp
	private Timestamp createdOn;

	@ManyToOne
	@JoinColumn(name = "lead_created_by", columnDefinition = "id")
	private UserProfile leadCreatedBy;

	@Column(name = "updated_on")
	@UpdateTimestamp
	private Timestamp updatedOn;

	@Transient
	@JsonIgnore
	private List<String> leadSourceIds;

	@Transient
	@JsonIgnore
	private List<String> status;
}
