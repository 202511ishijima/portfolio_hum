package com.ishijima.portfoliobackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CafeSeatView {
	private String seatNo;
	private String seatType;
	private Integer capacity;
	private boolean occupied;
}

