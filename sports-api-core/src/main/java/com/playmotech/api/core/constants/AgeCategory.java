package com.playmotech.api.core.constants;

public enum AgeCategory {
	UNDER_6("Under 6"), UNDER_8("Under 8"), UNDER_10("Under 10"), UNDER_12("Under 12"), UNDER_14("Under 14"),
	UNDER_16("Under 16"), UNDER_18("Under 18"), UNDER_19("Under 19"), UNDER_23("Under 23"), SENIORS("Seniors");

	private final String label;

	AgeCategory(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
