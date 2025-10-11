package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.PaymentSchedule;

public class PaymentScheduleTypeConverter implements DynamoDBTypeConverter<String, PaymentSchedule> {

	@Override
	public String convert(PaymentSchedule paymentSchedule) {
		return paymentSchedule.name();
	}

	@Override
	public PaymentSchedule unconvert(String s) {
		return PaymentSchedule.valueOf(s);
	}
}
