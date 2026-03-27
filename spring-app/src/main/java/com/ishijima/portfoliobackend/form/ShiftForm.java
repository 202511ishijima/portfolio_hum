package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ShiftForm(
	@NotNull(message = "Employee is required.")
	Long employeeId,

	@NotNull(message = "Work date is required.")
	LocalDate workDate,

	@NotNull(message = "Start time is required.")
	LocalTime startTime,

	@NotNull(message = "End time is required.")
	LocalTime endTime,

	@Size(max = 500, message = "Note must be 500 characters or less.")
	String note
) {
}
