package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryReplyForm(
	@NotBlank(message = "返信内容を入力してください。")
	@Size(max = 2000, message = "返信内容は2000文字以内で入力してください。")
	String reply
) {
}
