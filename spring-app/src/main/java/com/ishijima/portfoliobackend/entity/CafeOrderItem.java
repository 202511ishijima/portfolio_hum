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
public class CafeOrderItem {
	private Long id;
	private Long orderId;
	private String menuId;
	private String menuName;
	private Integer unitPrice;
	private Integer quantity;
	private Integer lineTotal;
	private LocalDateTime createdAt;
}

