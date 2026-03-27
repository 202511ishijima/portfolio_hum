package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageForm(
	@NotBlank(message = "Message is required.")
	@Size(max = 1000, message = "Message must be 1000 characters or less.")
	String body
) {
}
