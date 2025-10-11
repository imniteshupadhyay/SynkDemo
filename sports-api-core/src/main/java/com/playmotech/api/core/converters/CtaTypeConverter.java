package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.CtaType;

public class CtaTypeConverter implements DynamoDBTypeConverter<String, CtaType> {

	@Override
	public String convert(CtaType ctaType) {
		return ctaType.name();
	}

	@Override
	public CtaType unconvert(String s) {
		return CtaType.valueOf(s);
	}
}
