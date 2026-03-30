package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CafeMenuUpdateForm {

	@NotNull(message = "価格を入力してください。")
	@Min(value = 0, message = "価格は0以上で入力してください。")
	private Integer price;

	private boolean available;

	public Integer getPrice() {
		return price;
	}

	public void setPrice(Integer price) {
		this.price = price;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}
}

