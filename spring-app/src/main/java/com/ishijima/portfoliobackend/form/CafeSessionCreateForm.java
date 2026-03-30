package com.ishijima.portfoliobackend.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CafeSessionCreateForm {

	@NotBlank(message = "座席番号を入力してください。")
	private String seatNo;

	@Min(value = 1, message = "人数は1以上で入力してください。")
	private Integer guestCount;

	public String getSeatNo() {
		return seatNo;
	}

	public void setSeatNo(String seatNo) {
		this.seatNo = seatNo;
	}

	public Integer getGuestCount() {
		return guestCount;
	}

	public void setGuestCount(Integer guestCount) {
		this.guestCount = guestCount;
	}
}

