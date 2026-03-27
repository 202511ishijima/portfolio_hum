package com.ishijima.portfoliobackend.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class ProductOrderBulkForm {

	@NotEmpty(message = "発注対象がありません。")
	@Valid
	private List<ProductOrderLineForm> items = new ArrayList<>();

	public List<ProductOrderLineForm> getItems() {
		return items;
	}

	public void setItems(List<ProductOrderLineForm> items) {
		this.items = items;
	}
}
