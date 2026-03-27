package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ProductOrderLineForm {

	@NotBlank(message = "商品IDを入力してください。")
	private String productId;

	@Min(value = 0, message = "発注数は0以上で入力してください。")
	private Integer quantity;

	@Min(value = 1, message = "推奨在庫数は1以上で入力してください。")
	private Integer recommendedStock;

	public ProductOrderLineForm() {
	}

	public ProductOrderLineForm(String productId, Integer quantity, Integer recommendedStock) {
		this.productId = productId;
		this.quantity = quantity;
		this.recommendedStock = recommendedStock;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Integer getRecommendedStock() {
		return recommendedStock;
	}

	public void setRecommendedStock(Integer recommendedStock) {
		this.recommendedStock = recommendedStock;
	}
}
