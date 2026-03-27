package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.form.EmployeeCreateForm;
import com.ishijima.portfoliobackend.form.EmployeeUpdateForm;

import java.util.List;
import java.util.Optional;

public interface EmployeeService {

	Employee create(EmployeeCreateForm form, String actorLoginId);

	List<Employee> findAll();

	Employee findById(Long id);

	Optional<Employee> findByEmail(String email);

	Employee update(Long id, EmployeeUpdateForm form, String actorLoginId);

	void toggleActive(Long id, String actorLoginId);

	List<String> getAssignablePositions(String actorLoginId);

	boolean canManagePosition(String actorLoginId, String targetPosition);

	boolean isHeadOffice(String actorLoginId);
}
