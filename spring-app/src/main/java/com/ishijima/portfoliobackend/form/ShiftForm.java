package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ShiftForm(
	@NotNull(message = "Employee is required.")
	Long employeeId,

	@NotNull(message = "Work date is required.")
	LocalDate workDate,

	@NotBlank(message = "Shift slot is required.")
	String shiftSlot,

	@Size(max = 500, message = "Note must be 500 characters or less.")
	String note
) {
}
