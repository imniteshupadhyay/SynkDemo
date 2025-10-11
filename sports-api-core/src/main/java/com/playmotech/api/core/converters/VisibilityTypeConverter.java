package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.Visibility;

public class VisibilityTypeConverter implements DynamoDBTypeConverter<String, Visibility> {

	@Override
	public String convert(Visibility visibility) {
		return visibility.name();
	}

	@Override
	public Visibility unconvert(String s) {
		return Visibility.valueOf(s);
	}
}
