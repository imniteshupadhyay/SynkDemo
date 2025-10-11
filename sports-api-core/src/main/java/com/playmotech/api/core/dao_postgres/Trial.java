package com.playmotech.api.core.dao_postgres;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "trial")
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Trial {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private String id;

	@Column(name = "name")
	private String name;

	@Column(name = "address")
	private String address;

	@Column(name = "reason")
	private String reason;

	@OneToMany(mappedBy = "trial", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference
	private List<TrialFeedback> feedback;

	@Column(name = "trail_date")
	private LocalDate trailDate;

	@Column(name = "dob")
	private LocalDate dob;

	@Column(name = "trail_time")
	private LocalTime trailTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender")
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private TrialStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "sports")
	private Sports sports;

	@Column(name = "email")
	private String email;

	@Column(name = "phone")
	private String phone;

	@Column(name = "lead_id")
	private String leadId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coach_id", referencedColumnName = "id")
	private UserProfile coach;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_id", referencedColumnName = "id")
	private UserProfile createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "updated_id", referencedColumnName = "id")
	private UserProfile updatedBy;

	@JsonIgnore
	@Column(name = "deleted")
	private Boolean deleted;

	@Column(name = "completed")
	private boolean completed;

	@JsonIgnore
	@CreationTimestamp
	@Column(name = "inserted_on", updatable = false)
	private LocalDateTime insertedOn;

	@JsonIgnore
	@UpdateTimestamp
	@Column(name = "updated_on", insertable = false)
	private LocalDateTime updatedOn;

	public enum TrialStatus {
		PENDING, REJECTED, SCHEDULED, SELECTED
	}
}
