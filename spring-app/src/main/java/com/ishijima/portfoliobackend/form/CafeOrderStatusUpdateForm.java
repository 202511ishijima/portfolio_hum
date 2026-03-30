package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.NotBlank;

public class CafeOrderStatusUpdateForm {

	@NotBlank(message = "ステータスを選択してください。")
	private String status;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}

