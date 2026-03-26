package com.ishijima.portfoliobackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HamsterMonthlyAnalyticsPoint {

	private final String monthLabel;
	private final int totalCount;
	private final int availableCount;
	private final int negotiatingCount;
	private final int adoptedCount;
}
