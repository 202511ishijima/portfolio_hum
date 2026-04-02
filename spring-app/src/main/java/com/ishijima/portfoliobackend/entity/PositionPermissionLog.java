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
public class PositionPermissionLog {
	private Long id;
	private String position;
	private String changedBy;
	private String summary;
	private LocalDateTime changedAt;
}
