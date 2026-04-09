package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CafeCustomerIssueForm {

	@NotNull(message = "人数を入力してください。")
	@Min(value = 1, message = "人数は1名以上で入力してください。")
	@Max(value = 4, message = "1回の受付は4名までにしてください。")
	private Integer guestCount;

	@NotBlank(message = "席種を選択してください。")
	private String seatPreference;

	public Integer getGuestCount() {
		return guestCount;
	}

	public void setGuestCount(Integer guestCount) {
		this.guestCount = guestCount;
	}

	public String getSeatPreference() {
		return seatPreference;
	}

	public void setSeatPreference(String seatPreference) {
		this.seatPreference = seatPreference;
	}
}
