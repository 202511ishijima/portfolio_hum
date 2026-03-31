package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.PositionPermission;
import com.ishijima.portfoliobackend.mapper.EmployeeMapper;
import com.ishijima.portfoliobackend.mapper.PositionPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PositionPermissionServiceImpl implements PositionPermissionService {

	private final PositionPermissionMapper positionPermissionMapper;
	private final EmployeeMapper employeeMapper;

	@Override
	public List<PositionPermission> findAll() {
		return positionPermissionMapper.findAll();
	}

	@Override
	public Optional<PositionPermission> findByPosition(String position) {
		if (position == null || position.isBlank()) {
			return Optional.empty();
		}
		return positionPermissionMapper.findByPosition(position.trim());
	}

	@Override
	public String resolveRoleByPosition(String position) {
		return defaultRoleByPosition(position);
	}

	@Override
	@Transactional
	public void update(PositionPermission permission, String actorLoginId) {
		Employee actor = employeeMapper.findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("Actor not found."));
		if (!"本部".equals(actor.getPosition())) {
			throw new IllegalArgumentException("Only head office can update position permissions.");
		}
		if (permission == null || permission.getPosition() == null || permission.getPosition().isBlank()) {
			throw new IllegalArgumentException("Position is required.");
		}

		permission.setCanDashboard(Boolean.TRUE.equals(permission.getCanDashboard()));
		permission.setCanInquiries(Boolean.TRUE.equals(permission.getCanInquiries()));
		permission.setCanMembers(Boolean.TRUE.equals(permission.getCanMembers()));
		permission.setCanEmployees(Boolean.TRUE.equals(permission.getCanEmployees()));
		permission.setCanShifts(Boolean.TRUE.equals(permission.getCanShifts()));
		permission.setCanHamsters(Boolean.TRUE.equals(permission.getCanHamsters()));
		permission.setCanProducts(Boolean.TRUE.equals(permission.getCanProducts()));
		permission.setCanCafe(Boolean.TRUE.equals(permission.getCanCafe()));
		permission.setDefaultRole(defaultRoleByPosition(permission.getPosition()));
		permission.setUpdatedAt(LocalDateTime.now());

		positionPermissionMapper.upsert(permission);
	}

	private String defaultRoleByPosition(String position) {
		if (position == null) {
			return "STAFF";
		}
		return switch (position) {
			case "本部" -> "ADMIN";
			case "店長", "副店長" -> "STAFF_MANAGER";
			case "リーダー", "一般従業員" -> "STAFF";
			default -> "STAFF";
		};
	}
}
