package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberLoginForm(
	@NotBlank(message = "メールアドレスを入力してください。")
	@Email(message = "メールアドレスの形式で入力してください。")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
	String email,

	@NotBlank(message = "パスワードを入力してください。")
	@Size(max = 255, message = "パスワードは255文字以内で入力してください。")
	String password
) {
}
