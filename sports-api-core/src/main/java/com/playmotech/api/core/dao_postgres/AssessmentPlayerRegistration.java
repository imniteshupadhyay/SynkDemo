package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playmotech.api.core.constants.Gender;

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
@Table(name = "assessment_player_registration")
public class AssessmentPlayerRegistration {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "registration_id")
	private String registrationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assessment_id", nullable = false)
	private Assessment assessment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "player_id", referencedColumnName = "id", nullable = false)
	private UserProfile player;

	@Column(name = "registration_date", nullable = false)
	private LocalDate registrationDate;

	@Column(name = "registration_number", nullable = false)
	private String registrationNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "player_status", nullable = false)
	private PlayerStatus playerStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender")
	private Gender gender;

	// ✅ Payment fields
	@Column(name = "payment_amount")
	private Double paymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false)
	private RegistrationPaymentStatus paymentStatus;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@CreationTimestamp
	@Column(name = "created_on", nullable = false, updatable = false)
	private Timestamp createdOn;

	@Column(name = "updated_by")
	private String updatedBy;

	@UpdateTimestamp
	@Column(name = "updated_on", insertable = false)
	private Timestamp updatedOn;

	@Transient
	@JsonIgnore
	private String assessmentId;

	@Transient
	@JsonIgnore
	private List<String> playerStatuses;

	@Transient
	@JsonIgnore
	private List<String> paymentStatuses;

	// Player Status enum
	public enum PlayerStatus {
		REGISTERED, COMPLETED, WITHDRAWN, NO_SHOW
	}

	// ✅ Payment Status enum
	public enum RegistrationPaymentStatus {
		PENDING, PAID, REFUNDED
	}
}
