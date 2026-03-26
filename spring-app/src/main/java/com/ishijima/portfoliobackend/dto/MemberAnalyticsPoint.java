package com.ishijima.portfoliobackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberAnalyticsPoint {

	private final String label;
	private final long count;
}
