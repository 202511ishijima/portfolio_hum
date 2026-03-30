package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CafeAutoAssignForm {

	@NotNull(message = "人数を選択してください。")
	@Min(value = 1, message = "人数は1以上で入力してください。")
	@Max(value = 4, message = "1回の受付は4名まで対応しています。")
	private Integer guestCount;

	public Integer getGuestCount() {
		return guestCount;
	}

	public void setGuestCount(Integer guestCount) {
		this.guestCount = guestCount;
	}
}

