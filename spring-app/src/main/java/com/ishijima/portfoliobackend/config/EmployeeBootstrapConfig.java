package com.ishijima.portfoliobackend.config;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EmployeeBootstrapConfig {

	private final EmployeeMapper employeeMapper;
	private final PasswordEncoder passwordEncoder;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void createDefaultAdminIfMissing() {
		var existing = employeeMapper.findByEmail("admin@local");
		if (existing.isPresent()) {
			Employee employee = existing.get();
			if (!"本部".equals(employee.getPosition()) || !"ADMIN".equals(employee.getRole())) {
				employee.setPosition("本部");
				employee.setRole("ADMIN");
				employee.setUpdatedAt(LocalDateTime.now());
				employeeMapper.update(employee);
			}
			return;
		}

		Employee admin = Employee.builder()
			.name("Default Admin")
			.email("admin@local")
			.password(passwordEncoder.encode("admin1234"))
			.position("本部")
			.role("ADMIN")
			.active(true)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		employeeMapper.insert(admin);
	}
}
