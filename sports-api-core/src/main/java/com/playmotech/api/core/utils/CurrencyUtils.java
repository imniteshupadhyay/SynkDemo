package com.playmotech.api.core.utils;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.playmotech.api.core.constants.Currency;

public class CurrencyUtils {
	public static String formatCurrency(Currency currency, Long amount) {
		if (currency == Currency.INR) {
			// Get the currency instance for India
			java.util.Currency curr = java.util.Currency.getInstance(new Locale("en", "IN"));

			// Format the amount with commas and currency symbol
			NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
			formatter.setCurrency(curr);
			String formattedAmount = formatter.format(amount);
			if (StringUtils.isNotEmpty(formattedAmount)) {
				return formattedAmount.substring(0, formattedAmount.indexOf('.'));
			}
			return formatter.format(amount);
		} else {
			throw new UnsupportedOperationException("Currency not supported");
		}
	}
}
