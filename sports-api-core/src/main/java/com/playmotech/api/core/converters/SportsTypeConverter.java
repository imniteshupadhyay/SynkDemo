package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.Sports;

public class SportsTypeConverter implements DynamoDBTypeConverter<String, Sports> {

	@Override
	public String convert(Sports sports) {
		return sports.name();
	}

	@Override
	public Sports unconvert(String s) {
		return Sports.valueOf(s);
	}
}
