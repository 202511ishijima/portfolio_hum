package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotNull;

public record ProductStockAdjustForm(
	@NotNull(message = "調整数を入力してください。")
	Integer delta
) {
}
