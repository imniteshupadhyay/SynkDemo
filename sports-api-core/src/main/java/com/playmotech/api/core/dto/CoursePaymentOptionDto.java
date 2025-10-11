package com.playmotech.api.core.dto;

import com.playmotech.api.core.constants.Currency;
import com.playmotech.api.core.constants.PaymentSchedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursePaymentOptionDto {
    private Long id;
    private PaymentSchedule paymentSchedule;
    private Long paymentAmount;
    private Currency currency;
}
