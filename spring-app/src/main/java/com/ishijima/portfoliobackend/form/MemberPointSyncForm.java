package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberPointSyncForm(
	@NotBlank(message = "メールアドレスを入力してください。")
	@Email(message = "メールアドレスの形式で入力してください。")
	String email,

	@NotNull(message = "ポイント数を入力してください。")
	Integer delta
) {
}
