package com.playmotech.api.core.dao_postgres;

import java.sql.Timestamp;
import java.util.Map;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentCategory;
import com.playmotech.api.core.constants.PaymentSchedule;
import com.playmotech.api.core.constants.PaymentStatus;
import com.playmotech.api.core.repo.converters.PaymentExtraArgsConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {
	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "trainee_course_enrollment_id", referencedColumnName = "id")
	private TraineeCourseEnrollment traineeCourseEnrollment;
	@Column(name = "external_transaction_id")
	private String externalTransactionId;
	@Column(name = "payment_mode")
	private String paymentMode;
	@Column(name = "transaction_time")
	private String transactionTime;
	@Column(name = "payment_status")
	@Enumerated(value = EnumType.STRING)
	private PaymentStatus paymentStatus;
	@Column(name = "amount")
	private Long amount;
	@Column(name = "currency")
	@Enumerated(value = EnumType.STRING)
	private Currency currency;
	@Column(name = "payment_schedule")
	@Enumerated(value = EnumType.STRING)
	private PaymentSchedule paymentSchedule;
	@Column(name = "payment_category")
	@Enumerated(value = EnumType.STRING)
	private PaymentCategory paymentCategory;
	@Column(name = "payment_installment_date")
	private String paymentInstallmentDate;
	@Column(name = "next_due_at")
	private String nextDueAt;
	@Column(name = "created_at")
	private Timestamp createdAt;
	@Column(name = "updated_at")
	private Timestamp updatedAt;
	@Column(name = "extra_args")
	@Convert(converter = PaymentExtraArgsConverter.class)
	private Map<String, String> extraArgs;
	@Column(name = "receipt_id")
	private String receiptId;
	@ManyToOne
	@JoinColumn(name = "payment_initiated_by_user_id", referencedColumnName = "id")
	private UserProfile paymentInitiatedByUserProfile;
	@Column(name = "receipt_url")
	private String receiptUrl;
}