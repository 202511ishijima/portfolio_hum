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
public class CafeVisitSession {
	private Long id;
	private String sessionToken;
	private String seatNo;
	private Integer guestCount;
	private CafeVisitSessionStatus status;
	private LocalDateTime issuedAt;
	private LocalDateTime expiresAt;
	private LocalDateTime checkoutCompletedAt;
	private LocalDateTime updatedAt;
}

