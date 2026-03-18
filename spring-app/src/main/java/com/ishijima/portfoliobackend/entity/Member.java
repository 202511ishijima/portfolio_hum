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
public class Member {
	private Long id;
	private String name;
	private String email;
	private String password;
	private String status;
	private Integer points;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
