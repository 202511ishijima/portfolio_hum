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
public class CafeOrder {
	private Long id;
	private Long visitSessionId;
	private String seatNo;
	private CafeOrderStatus status;
	private Integer subtotal;
	private Integer tax;
	private Integer total;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime paidAt;
}
