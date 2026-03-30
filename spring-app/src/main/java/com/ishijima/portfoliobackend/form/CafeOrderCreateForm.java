package com.ishijima.portfoliobackend.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class CafeOrderCreateForm {

	@NotBlank(message = "セッションが不正です。")
	private String sessionToken;

	@Valid
	@NotEmpty(message = "注文商品を1件以上選択してください。")
	private List<Item> items = new ArrayList<>();

	public String getSessionToken() {
		return sessionToken;
	}

	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}

	public static class Item {
		@NotBlank(message = "商品IDが不正です。")
		private String menuId;

		@Min(value = 1, message = "数量は1以上で入力してください。")
		private Integer quantity;

		public String getMenuId() {
			return menuId;
		}

		public void setMenuId(String menuId) {
			this.menuId = menuId;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}
	}
}
