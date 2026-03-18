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
public class Inquiry {
	private Long id;
	private String name;
	private String email;
	private String subject;
	private String message;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
