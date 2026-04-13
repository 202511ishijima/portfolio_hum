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
public class PositionPermission {
	private String position;
	private String defaultRole;
	private Boolean canDashboard;
	private Boolean canInquiries;
	private Boolean canMembers;
	private Boolean canEmployees;
	private Boolean canShifts;
	private Boolean canHamsters;
	private Boolean canProducts;
	private Boolean canCafeCustomer;
	private Boolean canCafe;
	private LocalDateTime updatedAt;
}
