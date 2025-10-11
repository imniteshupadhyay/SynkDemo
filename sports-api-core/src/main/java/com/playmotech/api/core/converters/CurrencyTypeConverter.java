package com.playmotech.api.core.converters;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.playmotech.api.core.constants.Currency;

public class CurrencyTypeConverter implements DynamoDBTypeConverter<String, Currency> {

	@Override
	public String convert(Currency currency) {
		return currency.name();
	}

	@Override
	public Currency unconvert(String s) {
		return Currency.valueOf(s);
	}
}
