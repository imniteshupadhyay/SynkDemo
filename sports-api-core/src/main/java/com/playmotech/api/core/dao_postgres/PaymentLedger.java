package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.playmotech.api.core.constants.PaymentCategory;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "payment_ledger")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@ToString(exclude = { "user", "enrollment", "academy", "program", "payment" })
@EqualsAndHashCode(exclude = { "user", "enrollment", "academy", "program", "payment" })
public class PaymentLedger {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", referencedColumnName = "id")
	private UserProfile user;

	@ManyToOne
	@JoinColumn(name = "enrollment_id", referencedColumnName = "id")
	private TraineeCourseEnrollment enrollment;

	@ManyToOne
	@JoinColumn(name = "academy_id", referencedColumnName = "id")
	private Academy academy;

	@ManyToOne
	@JoinColumn(name = "program_id", referencedColumnName = "id")
	private Course program;

	@ManyToOne
	@JoinColumn(name = "payment_id", referencedColumnName = "id")
	private Payment payment;

	@Column(name = "effective_date")
	private LocalDate effectiveDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "ledger_type", nullable = false, columnDefinition = "VARCHAR(10) DEFAULT 'CREDIT'")
	private LedgerType ledgerType;

	@Enumerated(value = EnumType.STRING)
	@Column(name = "entry_type", nullable = false)
	private PaymentEntryType entryType;

	@Enumerated(value = EnumType.STRING)
	@Column(name = "entry_status", nullable = false, columnDefinition = "DEFAULT 'PENDING'")
	private PaymentEntryStatus entryStatus;

	@Enumerated(value = EnumType.STRING)
	@Column(name = "category", length = 50)
	private PaymentCategory category;

	@Column(name = "amount", nullable = false)
	private Double amount;

	@Column(name = "remaining_amount", nullable = false)
	private Double remainingAmount;

	@Column(name = "is_reversal", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean isReversal;

	@Column(name = "parent_entry_id")
	private Long parentEntryId;

	@Column(name = "is_locked", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private Boolean isLocked;

	@Column(name = "notes")
	private String notes;

	@Column(name = "description")
	private String description;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private Timestamp updatedAt;

	@ManyToOne
	@JoinColumn(name = "created_by", referencedColumnName = "id")
	private UserProfile createdBy;

	@ManyToOne
	@JoinColumn(name = "updated_by", referencedColumnName = "id")
	private UserProfile updatedBy;

	@ManyToOne
	@JoinColumn(name = "approved_by", referencedColumnName = "id")
	private UserProfile approvedBy;

	public enum PaymentEntryType {
		REGISTRATION, // For registration fee
		PAYMENT, // Normal payment received
		DISCOUNT, // Discount given
		ADVANCE, // Extra payment in advance
		CREDIT, // Extra payment in advance changed to credit
		PAST_DUE, // Scheduled due entry
		WAIVER, // Write-off
		ADJUSTMENT, // Manual adjustment
		REFUND, // Actual refund paid out
		REVERSAL // Reversal of a posted payment
	}

	public enum PaymentEntryStatus {
		PENDING, PARTIALLY_SETTLED, SETTLED, FAILED, CANCELLED, REFUNDED
	}

	public enum LedgerType {
		DEBIT, // Money in (academy receives)
		CREDIT, // Money out (academy pays)
		NEUTRAL // No net cash effect, for bookkeeping only
	}
}
