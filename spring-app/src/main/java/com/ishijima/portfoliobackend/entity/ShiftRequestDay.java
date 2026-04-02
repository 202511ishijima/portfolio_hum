package com.ishijima.portfoliobackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRequestDay {
	private Long id;
	private Long employeeId;
	private LocalDate requestDate;
	private String requestSlot;
	private Integer targetYear;
	private Integer targetMonth;
	private LocalDateTime updatedAt;
}
