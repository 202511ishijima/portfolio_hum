package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductPurchaseItemForm(
	@NotBlank(message = "商品IDを入力してください。")
	String productId,

	@Min(value = 1, message = "購入数量は1以上で入力してください。")
	Integer quantity
) {
}
