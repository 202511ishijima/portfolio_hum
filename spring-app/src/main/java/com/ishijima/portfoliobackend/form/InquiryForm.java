package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryForm(
	@NotBlank(message = "氏名は必須です。")
	@Size(max = 100, message = "氏名は100文字以内で入力してください。")
	String name,

	@NotBlank(message = "メールアドレスは必須です。")
	@Email(message = "メールアドレスの形式で入力してください。")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
	String email,

	@NotBlank(message = "件名は必須です。")
	@Size(max = 150, message = "件名は150文字以内で入力してください。")
	String subject,

	@NotBlank(message = "お問い合わせ本文は必須です。")
	@Size(max = 1000, message = "お問い合わせ本文は1000文字以内で入力してください。")
	String message
) {
}
