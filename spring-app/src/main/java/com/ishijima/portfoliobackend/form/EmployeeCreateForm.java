package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeCreateForm(
	@NotBlank(message = "氏名は必須です。")
	@Size(max = 100, message = "氏名は100文字以内で入力してください。")
	String name,

	@NotBlank(message = "ログインIDは必須です。")
	@Size(max = 255, message = "ログインIDは255文字以内で入力してください。")
	String email,

	@NotBlank(message = "パスワードは必須です。")
	@Size(min = 8, max = 255, message = "パスワードは8〜255文字で入力してください。")
	String password,

	@NotBlank(message = "役職は必須です。")
	@Size(max = 100, message = "役職は100文字以内で入力してください。")
	String position,

	@NotNull(message = "在籍状態は必須です。")
	Boolean active
) {
}
