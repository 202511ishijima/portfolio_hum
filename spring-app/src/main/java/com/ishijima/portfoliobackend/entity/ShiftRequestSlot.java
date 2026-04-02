package com.ishijima.portfoliobackend.entity;

public enum ShiftRequestSlot {
	NONE("希望なし"),
	OFF("休み"),
	EARLY("早番"),
	LATE("遅番"),
	FULL("終日"),
	FLEX("早番か遅番");

	private final String label;

	ShiftRequestSlot(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static ShiftRequestSlot from(String value) {
		if (value == null || value.isBlank()) {
			return NONE;
		}
		try {
			return ShiftRequestSlot.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return NONE;
		}
	}
}
