package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberUpdateForm(
	@NotBlank(message = "お名前を入力してください。")
	@Size(max = 100, message = "お名前は100文字以内で入力してください。")
	String name,

	@NotBlank(message = "メールアドレスを入力してください。")
	@Email(message = "メールアドレスの形式が正しくありません。")
	@Size(max = 255, message = "メールアドレスは255文字以内で入力してください。")
	String email,

	@Pattern(
		regexp = "^$|^.{8,255}$",
		message = "パスワードは8文字以上で入力してください。"
	)
	String password
) {
}
