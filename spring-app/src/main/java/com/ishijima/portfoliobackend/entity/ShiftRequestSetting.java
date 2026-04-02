package com.ishijima.portfoliobackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRequestSetting {
	private Long id;
	private Long employeeId;
	private Integer targetYear;
	private Integer targetMonth;
	private Integer weeklyDays;
	private LocalDateTime updatedAt;
}
