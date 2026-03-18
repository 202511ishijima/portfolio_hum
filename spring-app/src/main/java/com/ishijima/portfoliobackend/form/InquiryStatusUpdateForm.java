package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotBlank;

public record InquiryStatusUpdateForm(
	@NotBlank(message = "対応状況は必須です。")
	String status
) {
}
