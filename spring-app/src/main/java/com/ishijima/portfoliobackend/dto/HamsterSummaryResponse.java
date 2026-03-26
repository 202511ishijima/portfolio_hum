package com.ishijima.portfoliobackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HamsterSummaryResponse {

	private String species;
	private int count;
	private int maleCount;
	private int femaleCount;
	private String ageRange;
}
