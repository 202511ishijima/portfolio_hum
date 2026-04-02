package com.ishijima.portfoliobackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shift {
	private Long id;
	private Long employeeId;
	private String employeeName;
	private LocalDate workDate;
	private String shiftSlot;
	private LocalTime startTime;
	private LocalTime endTime;
	private String note;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public ShiftSlot getShiftSlotEnum() {
		return ShiftSlot.from(shiftSlot);
	}

	public String getShiftSlotLabel() {
		return getShiftSlotEnum().getLabel();
	}

	public String getShiftSlotShortLabel() {
		return switch (getShiftSlotEnum()) {
			case EARLY -> "早";
			case LATE -> "遅";
			case FULL -> "終";
		};
	}
}
