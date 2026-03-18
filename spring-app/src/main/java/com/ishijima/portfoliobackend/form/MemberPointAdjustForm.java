package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotNull;

public record MemberPointAdjustForm(
	@NotNull(message = "ポイント数を入力してください。")
	Integer delta
) {
}
