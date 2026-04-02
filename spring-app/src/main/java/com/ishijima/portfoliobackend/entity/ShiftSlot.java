package com.ishijima.portfoliobackend.entity;

import java.time.LocalTime;

public enum ShiftSlot {
	EARLY("\u65e9\u756a", LocalTime.of(10, 0), LocalTime.of(15, 0)),
	LATE("\u9045\u756a", LocalTime.of(15, 0), LocalTime.of(21, 0)),
	FULL("\u7d42\u65e5", LocalTime.of(10, 0), LocalTime.of(21, 0));

	private final String label;
	private final LocalTime startTime;
	private final LocalTime endTime;

	ShiftSlot(String label, LocalTime startTime, LocalTime endTime) {
		this.label = label;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public String getLabel() {
		return label;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public static ShiftSlot from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Shift slot is required.");
		}
		try {
			return ShiftSlot.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid shift slot: " + value);
		}
	}
}
