package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateForm(
	@NotBlank(message = "お名前を入力してください。")
	@Size(max = 100, message = "お名前は100文字以内で入力してください。")
	String name,

	@NotBlank(message = "メールアドレスを入力してください。")
	@Email(message = "メールアドレスの形式で入力してください。")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
	String email,

	@NotBlank(message = "パスワードを入力してください。")
	@Size(min = 8, max = 255, message = "パスワードは8文字以上255文字以内で入力してください。")
	String password
) {
}
