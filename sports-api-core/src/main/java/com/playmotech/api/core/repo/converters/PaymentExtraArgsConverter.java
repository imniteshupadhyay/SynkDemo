package com.playmotech.api.core.repo.converters;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.reflect.TypeToken;
import com.playmotech.api.core.constants.AppConstants;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentExtraArgsConverter implements AttributeConverter<Map<String, String>, String> {

	@Override
	public String convertToDatabaseColumn(Map<String, String> stringStringMap) {
		if (stringStringMap != null) {
			return AppConstants.GSON.toJson(stringStringMap);
		}
		return null;
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String s) {
		if (StringUtils.isNotEmpty(s)) {
			return AppConstants.GSON.fromJson(s,
					TypeToken.getParameterized(Map.class, String.class, String.class).getType());
		}
		return Map.of();
	}
}
