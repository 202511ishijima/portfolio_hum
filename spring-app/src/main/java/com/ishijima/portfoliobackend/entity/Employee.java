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
public class Employee {
	private Long id;
	private String name;
	private String email;
	private String password;
	private String position;
	private String role;
	private Boolean active;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
