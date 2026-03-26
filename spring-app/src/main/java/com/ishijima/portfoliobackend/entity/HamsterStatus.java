package com.ishijima.portfoliobackend.entity;

public enum HamsterStatus {

	AVAILABLE("在籍中"),
	NEGOTIATING("商談中"),
	ADOPTED("お迎え済み");

	private final String label;

	HamsterStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public static HamsterStatus fromDatabaseValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		for (HamsterStatus status : values()) {
			if (status.name().equalsIgnoreCase(value) || status.label.equals(value)) {
				return status;
			}
		}

		return null;
	}
}
