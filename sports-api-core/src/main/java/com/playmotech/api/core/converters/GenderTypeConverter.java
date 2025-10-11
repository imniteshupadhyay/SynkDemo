package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.Gender;

public class GenderTypeConverter implements DynamoDBTypeConverter<String, Gender> {

	@Override
	public String convert(Gender gender) {
		return gender.name();
	}

	@Override
	public Gender unconvert(String s) {
		return Gender.valueOf(s);
	}
}
