package com.playmotech.api.core.dao_postgres;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentSchedule;

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
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course_payments_option_mappings")
@Data
@Builder
public class CoursePaymentOptionsMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JsonBackReference
	@JoinColumn(name = "course_id", referencedColumnName = "id")
	private Course course;

	@Column(name = "payment_schedule")
	@Enumerated(value = EnumType.STRING)
	private PaymentSchedule paymentSchedule;
	@Column(name = "payment_amount")
	private Long paymentAmount;
	@Column(name = "currency")
	@Enumerated(value = EnumType.STRING)
	private Currency currency;
}
