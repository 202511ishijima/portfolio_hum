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

	private static final Map<String, Integer> POSITION_RANK = new LinkedHashMap<>();

	static {
		POSITION_RANK.put("本部", 5);
		POSITION_RANK.put("店長", 4);
		POSITION_RANK.put("副店長", 3);
		POSITION_RANK.put("リーダー", 2);
		POSITION_RANK.put("一般従業員", 1);
	}

	@Override
	@Transactional
	public Employee create(EmployeeCreateForm form, String actorLoginId) {
		validatePosition(form.position());
		assertCanAssign(actorLoginId, form.position());
		employeeMapper.findByEmail(form.email()).ifPresent(existing -> {
			throw new IllegalArgumentException("This login ID is already in use.");
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
			.orElseThrow(() -> new IllegalArgumentException("Employee not found. id=" + id));
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
				throw new IllegalArgumentException("This login ID is already in use.");
			}
		});

		employee.setName(form.name());
		employee.setEmail(form.email());
		employee.setPosition(form.position());
		employee.setRole(mapRoleFromPosition(form.position()));
		employee.setActive(form.active());
		if (form.password() != null && !form.password().isBlank()) {
			if (form.password().length() < 8) {
				throw new IllegalArgumentException("Password must be at least 8 characters.");
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
	public List<String> getAssignablePositions(String actorLoginId) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("Actor not found."));
		Integer actorRank = POSITION_RANK.get(actor.getPosition());
		if (actorRank == null) {
			throw new IllegalArgumentException("Invalid actor position.");
		}
		return POSITION_RANK.entrySet().stream()
			.filter(entry -> entry.getValue() < actorRank)
			.map(Map.Entry::getKey)
			.toList();
	}

	@Override
	public boolean canManagePosition(String actorLoginId, String targetPosition) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("Actor not found."));
		Integer actorRank = POSITION_RANK.get(actor.getPosition());
		Integer targetRank = POSITION_RANK.get(targetPosition);
		return actorRank != null && targetRank != null && targetRank < actorRank;
	}

	@Override
	public boolean isHeadOffice(String actorLoginId) {
		Employee actor = findByEmail(actorLoginId)
			.orElseThrow(() -> new IllegalArgumentException("Actor not found."));
		Integer actorRank = POSITION_RANK.get(actor.getPosition());
		return actorRank != null && actorRank == 5;
	}

	private void assertCanAssign(String actorLoginId, String targetPosition) {
		if (!canManagePosition(actorLoginId, targetPosition)) {
			throw new IllegalArgumentException("You can assign only positions below your own.");
		}
	}

	private void validatePosition(String position) {
		if (!POSITION_RANK.containsKey(position)) {
			throw new IllegalArgumentException("Invalid position.");
		}
	}

	private String mapRoleFromPosition(String position) {
		return switch (position) {
			case "本部" -> "ADMIN";
			case "店長", "副店長" -> "STAFF_MANAGER";
			case "リーダー", "一般従業員" -> "STAFF";
			default -> throw new IllegalArgumentException("Invalid position.");
		};
	}
}
