package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeUserDetailsService implements UserDetailsService {

	private final EmployeeService employeeService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Employee employee = employeeService.findByEmail(username)
			.orElseThrow(() -> new UsernameNotFoundException("Employee not found."));

		List<GrantedAuthority> authorities = List.of(
			new SimpleGrantedAuthority("ROLE_" + employee.getRole())
		);

		return User.builder()
			.username(employee.getEmail())
			.password(employee.getPassword())
			.disabled(!Boolean.TRUE.equals(employee.getActive()))
			.authorities(authorities)
			.build();
	}
}
