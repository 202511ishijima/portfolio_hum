package com.ishijima.portfoliobackend.entity;

public enum HamsterSex {

	MALE("オス"),
	FEMALE("メス");

	private final String label;

	HamsterSex(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static HamsterSex fromDatabaseValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		for (HamsterSex sex : values()) {
			if (sex.name().equalsIgnoreCase(value) || sex.label.equals(value)) {
				return sex;
			}
		}

		return null;
	}
}
