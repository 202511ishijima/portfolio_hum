package com.ishijima.portfoliobackend.form;

import com.ishijima.portfoliobackend.entity.HamsterSex;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class HamsterForm {

	@NotBlank(message = "種類を選択してください。")
	@Size(max = 100, message = "種類は100文字以内で入力してください。")
	private String species;

	@NotNull(message = "性別を選択してください。")
	private HamsterSex sex;

	@NotNull(message = "生年月日を入力してください。")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate birthDate;

	@NotNull(message = "入荷日を入力してください。")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate arrivalDate;

	@NotNull(message = "在籍状況を選択してください。")
	private HamsterStatus status;

	@NotNull(message = "登録匹数を入力してください。")
	@Min(value = 1, message = "登録匹数は1以上で入力してください。")
	@Max(value = 50, message = "登録匹数は50以下で入力してください。")
	private Integer quantity = 1;

	@Size(max = 1000, message = "備考は1000文字以内で入力してください。")
	private String notes;
}
