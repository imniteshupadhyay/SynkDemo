package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.GameFormat;

public class GameFormatTypeConverter implements DynamoDBTypeConverter<String, GameFormat> {

	@Override
	public String convert(GameFormat gameFormat) {
		return gameFormat.name();
	}

	@Override
	public GameFormat unconvert(String s) {
		return GameFormat.valueOf(s);
	}
}
