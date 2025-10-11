package com.playmotech.api.core.constants;

import java.util.Currency;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.playmotech.api.core.dto.VisibilityConfigDto;

public final class AppConstants {
	public final static String DEFAULT_TIMEZONE = "Asia/Kolkata";
	public final static Currency INR_CURRENCY = java.util.Currency.getInstance(new Locale("en", "IN"));
	public final static com.playmotech.api.core.constants.Currency DEFAULT_CURRENCY = com.playmotech.api.core.constants.Currency.INR;
	public final static Gson GSON = new Gson();
	public final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	public final static List<VisibilityConfigDto> VISIBILITY_CONFIGS = GSON.fromJson(
			"[{\"screen\":\"PlayerScreen\"," + "\"disableFeatures\":{\"addNewTrainee\":true,\"edit_delete\":true}},"
					+ "{\"screen\":\"CoachScreen\","
					+ "\"disableFeatures\":{\"addNewCoach\":true,\"edit_delete\":true}},"
					+ "{\"screen\":\"ProgramScreen\"," + "\"disableFeatures\":{\"payment\":true}},"
					+ "{\"screen\":\"DashboardReport\"," + "\"disableFeatures\":{\"dailyReport\":true}},"
					+ "{\"screen\":\"TraineeProfile\"," + "\"disableFeatures\":{\"payment\":true}},"
					+ "{\"screen\":\"Report\"," + "\"disableFeatures\":{\"paymentReport\":true}},"
					+ "{\"screen\":\"ProgramDetail\"," + "\"disableFeatures\":{\"addNewTrainee\":true}}" + "]",
			TypeToken.getParameterized(List.class, VisibilityConfigDto.class).getType());

	public final static List<VisibilityConfigDto> VISIBILITY_CONFIGS_CASH_PAYMENT = GSON.fromJson(
			"[{\"screen\":\"PlayerScreen\"," + "\"disableFeatures\":{\"addNewTrainee\":true,\"edit_delete\":true}},"
					+ "{\"screen\":\"CoachScreen\","
					+ "\"disableFeatures\":{\"addNewCoach\":true,\"edit_delete\":true}},"
					+ "{\"screen\":\"ProgramScreen\",\"disableFeatures\":{\"payment\":true}},"
					+ "{\"screen\":\"DashboardReport\"," + "\"disableFeatures\":{\"dailyReport\":true}},"
					+ "{\"screen\":\"TraineeProfile\",\"disableFeatures\":{\"payment\":true,\"cashPayment\":true}},"
					+ "{\"screen\":\"Report\",\"disableFeatures\":{\"paymentReport\":true}},"
					+ "{\"screen\":\"ProgramDetail\",\"disableFeatures\":{\"addNewTrainee\":true}}" + "]",
			TypeToken.getParameterized(List.class, VisibilityConfigDto.class).getType());

}
