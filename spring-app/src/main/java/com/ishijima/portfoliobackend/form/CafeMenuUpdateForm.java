package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CafeMenuUpdateForm {

	@NotBlank(message = "メニュー名を入力してください。")
	private String name;

	@NotNull(message = "価格を入力してください。")
	@Min(value = 0, message = "価格は0円以上で入力してください。")
	private Integer price;

	private boolean available;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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
