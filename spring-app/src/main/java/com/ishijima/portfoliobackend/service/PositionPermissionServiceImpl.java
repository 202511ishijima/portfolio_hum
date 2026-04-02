package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.PositionPermission;
import com.ishijima.portfoliobackend.entity.PositionPermissionLog;
import com.ishijima.portfoliobackend.mapper.EmployeeMapper;
import com.ishijima.portfoliobackend.mapper.PositionPermissionLogMapper;
import com.ishijima.portfoliobackend.mapper.PositionPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PositionPermissionServiceImpl implements PositionPermissionService {

	private final PositionPermissionMapper positionPermissionMapper;
	private final PositionPermissionLogMapper positionPermissionLogMapper;
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
		updateAll(List.of(permission), actorLoginId);
	}

	@Override
	@Transactional
	public void updateAll(List<PositionPermission> permissions, String actorLoginId) {
		Employee actor = employeeMapper.findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("Actor not found."));
		if (!"本部".equals(actor.getPosition())) {
			throw new IllegalArgumentException("Only head office can update position permissions.");
		}
		if (permissions == null || permissions.isEmpty()) {
			throw new IllegalArgumentException("No permission changes found.");
		}

		for (PositionPermission permission : permissions) {
			if (permission == null || permission.getPosition() == null || permission.getPosition().isBlank()) {
				continue;
			}

			PositionPermission normalized = normalize(permission);
			PositionPermission before = positionPermissionMapper.findByPosition(normalized.getPosition()).orElse(null);

			if (before != null && !hasChanged(before, normalized)) {
				continue;
			}

			positionPermissionMapper.upsert(normalized);
			String summary = buildSummary(before, normalized);
			positionPermissionLogMapper.insert(PositionPermissionLog.builder()
				.position(normalized.getPosition())
				.changedBy(actor.getName() != null && !actor.getName().isBlank() ? actor.getName() : actor.getEmail())
				.summary(summary)
				.changedAt(LocalDateTime.now())
				.build());
		}
	}

	@Override
	public List<PositionPermissionLog> findRecentLogs(int limit) {
		int safeLimit = limit <= 0 ? 20 : Math.min(limit, 200);
		return positionPermissionLogMapper.findRecent(safeLimit);
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

	private PositionPermission normalize(PositionPermission permission) {
		return PositionPermission.builder()
			.position(permission.getPosition().trim())
			.defaultRole(defaultRoleByPosition(permission.getPosition()))
			.canDashboard(Boolean.TRUE.equals(permission.getCanDashboard()))
			.canInquiries(Boolean.TRUE.equals(permission.getCanInquiries()))
			.canMembers(Boolean.TRUE.equals(permission.getCanMembers()))
			.canEmployees(Boolean.TRUE.equals(permission.getCanEmployees()))
			.canShifts(Boolean.TRUE.equals(permission.getCanShifts()))
			.canHamsters(Boolean.TRUE.equals(permission.getCanHamsters()))
			.canProducts(Boolean.TRUE.equals(permission.getCanProducts()))
			.canCafe(Boolean.TRUE.equals(permission.getCanCafe()))
			.updatedAt(LocalDateTime.now())
			.build();
	}

	private boolean hasChanged(PositionPermission before, PositionPermission after) {
		return !Boolean.TRUE.equals(before.getCanDashboard()) == Boolean.TRUE.equals(after.getCanDashboard())
			|| !Boolean.TRUE.equals(before.getCanInquiries()) == Boolean.TRUE.equals(after.getCanInquiries())
			|| !Boolean.TRUE.equals(before.getCanMembers()) == Boolean.TRUE.equals(after.getCanMembers())
			|| !Boolean.TRUE.equals(before.getCanEmployees()) == Boolean.TRUE.equals(after.getCanEmployees())
			|| !Boolean.TRUE.equals(before.getCanShifts()) == Boolean.TRUE.equals(after.getCanShifts())
			|| !Boolean.TRUE.equals(before.getCanHamsters()) == Boolean.TRUE.equals(after.getCanHamsters())
			|| !Boolean.TRUE.equals(before.getCanProducts()) == Boolean.TRUE.equals(after.getCanProducts())
			|| !Boolean.TRUE.equals(before.getCanCafe()) == Boolean.TRUE.equals(after.getCanCafe());
	}

	private String buildSummary(PositionPermission before, PositionPermission after) {
		if (before == null) {
			return "権限設定を新規作成しました。";
		}
		List<String> changes = new ArrayList<>();
		appendChange(changes, "ダッシュボード", before.getCanDashboard(), after.getCanDashboard());
		appendChange(changes, "お問い合わせ", before.getCanInquiries(), after.getCanInquiries());
		appendChange(changes, "会員管理", before.getCanMembers(), after.getCanMembers());
		appendChange(changes, "従業員管理", before.getCanEmployees(), after.getCanEmployees());
		appendChange(changes, "シフト管理", before.getCanShifts(), after.getCanShifts());
		appendChange(changes, "ハムスター管理", before.getCanHamsters(), after.getCanHamsters());
		appendChange(changes, "商品在庫管理", before.getCanProducts(), after.getCanProducts());
		appendChange(changes, "カフェ注文", before.getCanCafe(), after.getCanCafe());
		return changes.isEmpty() ? "変更なし" : String.join(" / ", changes);
	}

	private void appendChange(List<String> changes, String label, Boolean before, Boolean after) {
		boolean b = Boolean.TRUE.equals(before);
		boolean a = Boolean.TRUE.equals(after);
		if (b != a) {
			changes.add(label + ": " + (b ? "ON" : "OFF") + "→" + (a ? "ON" : "OFF"));
		}
	}
}
