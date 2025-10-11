package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.SkillLevel;

public class SkillLevelTypeConverter implements DynamoDBTypeConverter<String, SkillLevel> {

	@Override
	public String convert(SkillLevel skillLevel) {
		return skillLevel.name();
	}

	@Override
	public SkillLevel unconvert(String s) {
		return SkillLevel.valueOf(s);
	}
}
