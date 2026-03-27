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
public class ProductOrder {
	private Long id;
	private String productId;
	private Integer quantity;
	private String note;
	private String orderedBy;
	private LocalDateTime createdAt;
}
