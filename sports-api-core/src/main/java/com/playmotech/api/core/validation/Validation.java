package com.playmotech.api.core.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class Validation {

	public static boolean isNotEmpty(Object... values) {
		for (Object value : values) {
			if (value == null || StringUtils.isEmpty(value.toString())) {
				return false;
			}
		}
		return true;
	}

	public static boolean isValidLength(String input, int minLength, int maxLength) {
		return input != null && input.length() >= minLength && input.length() <= maxLength;
	}

	protected static boolean isValidLengthRegex(String value, String regex) {
		return value != null && value.matches(regex);
	}

	public boolean isValidId(Long id) {
		return id != null && id > 0;
	}

	public boolean validate(String token) {
		return isNotEmpty(token);
	}

	public boolean isValidStringId(String id) {
		return StringUtils.isNotBlank(id) && id.matches("^[a-zA-Z0-9_-]+$");
	}
}
