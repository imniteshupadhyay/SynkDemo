package com.playmotech.api.core.utils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

public class SpecificationUtil {

	public static final Predicate<Object> isDateNotNull = Objects::nonNull;

	public static final Predicate<String> isStringNotNullAndBlank = string -> string != null && !string.isEmpty()
			&& !string.isBlank();

	public static final Predicate<Number> isNumberNotNullAndNonZero = number -> number != null && !number.equals(0)
			&& !number.equals(0.0);

	public static final LongPredicate isLongNotNullAndNonZero = number -> Optional.ofNullable(number)
			.map(Long::longValue).filter(value -> value != 0L).isPresent();

	public static final Predicate<List<? extends Number>> isNumberListNotNullAndEmpty = list -> list != null
			&& !list.isEmpty();

}
