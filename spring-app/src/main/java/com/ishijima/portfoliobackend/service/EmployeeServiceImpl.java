package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.form.EmployeeCreateForm;
import com.ishijima.portfoliobackend.form.EmployeeUpdateForm;
import com.ishijima.portfoliobackend.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

	private final EmployeeMapper employeeMapper;
	private final PasswordEncoder passwordEncoder;
	private final PositionPermissionService positionPermissionService;

	private static final Map<String, Integer> POSITION_RANK = new LinkedHashMap<>();
	private static final Map<String, Integer> ROLE_RANK = new LinkedHashMap<>();

	static {
		POSITION_RANK.put("本部", 5);
		POSITION_RANK.put("店長", 4);
		POSITION_RANK.put("副店長", 3);
		POSITION_RANK.put("リーダー", 2);
		POSITION_RANK.put("一般従業員", 1);

		ROLE_RANK.put("ADMIN", 4);
		ROLE_RANK.put("STAFF_MANAGER", 3);
		ROLE_RANK.put("STAFF", 2);
		ROLE_RANK.put("VIEWER", 1);
	}

	@Override
	@Transactional
	public Employee create(EmployeeCreateForm form, String actorLoginId) {
		validatePosition(form.position());
		assertCanAssign(actorLoginId, form.position());
		employeeMapper.findByEmail(form.email()).ifPresent(existing -> {
			throw new IllegalArgumentException("このログインIDはすでに使用されています。");
		});

		Employee employee = Employee.builder()
			.name(form.name())
			.email(form.email())
			.password(passwordEncoder.encode(form.password()))
			.position(form.position())
			.role(mapRoleFromPosition(form.position()))
			.active(form.active())
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		employeeMapper.insert(employee);
		return employee;
	}

	@Override
	public List<Employee> findAll() {
		return employeeMapper.findAll();
	}

	@Override
	public Employee findById(Long id) {
		return employeeMapper.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("従業員が見つかりません。id=" + id));
	}

	@Override
	public Optional<Employee> findByEmail(String email) {
		return employeeMapper.findByEmail(email);
	}

	@Override
	@Transactional
	public Employee update(Long id, EmployeeUpdateForm form, String actorLoginId) {
		validatePosition(form.position());
		assertCanAssign(actorLoginId, form.position());
		Employee employee = findById(id);

		employeeMapper.findByEmail(form.email()).ifPresent(existing -> {
			if (!existing.getId().equals(id)) {
				throw new IllegalArgumentException("このログインIDはすでに使用されています。");
			}
		});

		employee.setName(form.name());
		employee.setEmail(form.email());
		employee.setPosition(form.position());
		employee.setRole(mapRoleFromPosition(form.position()));
		employee.setActive(form.active());
		if (form.password() != null && !form.password().isBlank()) {
			if (form.password().length() < 8) {
				throw new IllegalArgumentException("パスワードは8文字以上で入力してください。");
			}
			employee.setPassword(passwordEncoder.encode(form.password()));
		}
		employee.setUpdatedAt(LocalDateTime.now());

		employeeMapper.update(employee);
		return employee;
	}

	@Override
	@Transactional
	public void toggleActive(Long id, String actorLoginId) {
		Employee employee = findById(id);
		assertCanAssign(actorLoginId, employee.getPosition());
		boolean next = !Boolean.TRUE.equals(employee.getActive());
		employeeMapper.updateActive(id, next);
	}

	@Override
	@Transactional
	public void delete(Long id, String actorLoginId) {
		Employee target = findById(id);
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("操作ユーザーが見つかりません。"));

		if (actor.getId() != null && actor.getId().equals(target.getId())) {
			throw new IllegalArgumentException("自分自身のアカウントは削除できません。");
		}
		assertCanAssign(actorLoginId, target.getPosition());
		employeeMapper.deleteById(id);
	}

	@Override
	@Transactional
	public void updateRole(Long id, String role, String actorLoginId) {
		Employee target = findById(id);
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("操作ユーザーが見つかりません。"));

		if (actor.getId() != null && actor.getId().equals(target.getId())) {
			throw new IllegalArgumentException("自分自身の権限は変更できません。");
		}
		assertCanAssign(actorLoginId, target.getPosition());

		List<String> assignableRoles = getAssignableRoles(actorLoginId);
		if (role == null || !assignableRoles.contains(role)) {
			throw new IllegalArgumentException("自分より下位の権限のみ設定できます。");
		}
		employeeMapper.updateRole(id, role);
	}

	@Override
	public List<String> getAssignablePositions(String actorLoginId) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("操作ユーザーが見つかりません。"));
		Integer actorRank = POSITION_RANK.get(actor.getPosition());
		if (actorRank == null) {
			throw new IllegalArgumentException("操作ユーザーの役職が不正です。");
		}
		return POSITION_RANK.entrySet().stream()
			.filter(entry -> entry.getValue() < actorRank)
			.map(Map.Entry::getKey)
			.toList();
	}

	@Override
	public List<String> getAssignableRoles(String actorLoginId) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("操作ユーザーが見つかりません。"));
		Integer actorRank = ROLE_RANK.get(actor.getRole());
		if (actorRank == null) {
			throw new IllegalArgumentException("操作ユーザーの権限が不正です。");
		}
		return ROLE_RANK.entrySet().stream()
			.filter(entry -> entry.getValue() < actorRank)
			.map(Map.Entry::getKey)
			.toList();
	}

	@Override
	public boolean canManagePosition(String actorLoginId, String targetPosition) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("操作ユーザーが見つかりません。"));
		Integer actorRank = POSITION_RANK.get(actor.getPosition());
		Integer targetRank = POSITION_RANK.get(targetPosition);
		if (actorRank == null || targetRank == null) {
			return false;
		}
		if (actorRank == 5 && targetRank == 5) {
			return true;
		}
		return targetRank < actorRank;
	}

	@Override
	public boolean isHeadOffice(String actorLoginId) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("操作ユーザーが見つかりません。"));
		Integer actorRank = POSITION_RANK.get(actor.getPosition());
		return actorRank != null && actorRank == 5;
	}

	private void assertCanAssign(String actorLoginId, String targetPosition) {
		if (!canManagePosition(actorLoginId, targetPosition)) {
			throw new IllegalArgumentException("自分より下位の役職のみ設定できます。");
		}
	}

	private void validatePosition(String position) {
		if (!POSITION_RANK.containsKey(position)) {
			throw new IllegalArgumentException("役職が不正です。");
		}
	}

	private String mapRoleFromPosition(String position) {
		return positionPermissionService.resolveRoleByPosition(position);
	}
}
