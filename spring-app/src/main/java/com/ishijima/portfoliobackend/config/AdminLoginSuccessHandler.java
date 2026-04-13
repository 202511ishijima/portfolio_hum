package com.ishijima.portfoliobackend.config;

import com.ishijima.portfoliobackend.entity.PositionPermission;
import com.ishijima.portfoliobackend.mapper.EmployeeMapper;
import com.ishijima.portfoliobackend.service.PositionPermissionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AdminLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final EmployeeMapper employeeMapper;
	private final PositionPermissionService positionPermissionService;

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException, ServletException {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			response.sendRedirect("/admin/login");
			return;
		}

		String loginId = authentication.getName();
		String position = employeeMapper.findByEmail(loginId)
			.map(employee -> employee.getPosition())
			.orElse(null);
		PositionPermission permission = position == null
			? null
			: positionPermissionService.findByPosition(position).orElse(null);

		if (permission == null) {
			logoutAndRedirectToLogin(request, response);
			return;
		}

		if (Boolean.TRUE.equals(permission.getCanDashboard())) {
			response.sendRedirect("/admin/dashboard");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanInquiries())) {
			response.sendRedirect("/admin/inquiries");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanMembers())) {
			response.sendRedirect("/admin/members");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanEmployees())) {
			response.sendRedirect("/admin/employees");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanShifts())) {
			response.sendRedirect("/admin/shifts");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanHamsters())) {
			response.sendRedirect("/admin/hamsters");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanProducts())) {
			response.sendRedirect("/admin/products/stocks");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanCafeCustomer())) {
			response.sendRedirect("/admin/cafe/customer-screen");
			return;
		}
		if (Boolean.TRUE.equals(permission.getCanCafe())) {
			response.sendRedirect("/admin/cafe/orders");
			return;
		}

		logoutAndRedirectToLogin(request, response);
	}

	private void logoutAndRedirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SecurityContextHolder.clearContext();
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
		response.sendRedirect("/admin/login?noPermission");
	}
}
