package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.PaymentStatus;

public class PaymentStatusTypeConverter implements DynamoDBTypeConverter<String, PaymentStatus> {

	@Override
	public String convert(PaymentStatus paymentStatus) {
		return paymentStatus.name();
	}

	@Override
	public PaymentStatus unconvert(String s) {
		return PaymentStatus.valueOf(s);
	}
}
