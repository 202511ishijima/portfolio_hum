package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeUpdateForm(
	@NotBlank(message = "Name is required.")
	@Size(max = 100, message = "Name must be 100 characters or less.")
	String name,

	@NotBlank(message = "Login ID is required.")
	@Size(max = 255, message = "Login ID must be 255 characters or less.")
	String email,

	@Size(max = 255, message = "Password must be 255 characters or less.")
	String password,

	@NotBlank(message = "Position is required.")
	@Size(max = 100, message = "Position must be 100 characters or less.")
	String position,

	@NotNull(message = "Active status is required.")
	Boolean active
) {
}
