package com.ishijima.portfoliobackend.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductPurchaseForm(
	@NotEmpty(message = "購入商品がありません。")
	List<@Valid ProductPurchaseItemForm> items
) {
}
