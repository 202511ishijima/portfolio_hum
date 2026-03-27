package com.ishijima.portfoliobackend.mapper;

import com.ishijima.portfoliobackend.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EmployeeMapper {

	void insert(Employee employee);

	List<Employee> findAll();

	Optional<Employee> findById(@Param("id") Long id);

	Optional<Employee> findByEmail(@Param("email") String email);

	void update(Employee employee);

	void updateActive(@Param("id") Long id, @Param("active") boolean active);
}
