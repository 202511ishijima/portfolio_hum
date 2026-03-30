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
public class CafeMenu {
	private String id;
	private String category;
	private String name;
	private String description;
	private Integer price;
	private String image;
	private Boolean available;
	private Integer displayOrder;
	private LocalDateTime updatedAt;
}

