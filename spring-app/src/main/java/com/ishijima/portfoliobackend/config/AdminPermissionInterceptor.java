package com.ishijima.portfoliobackend.config;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.PositionPermission;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.PositionPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminPermissionInterceptor implements HandlerInterceptor {

	private final EmployeeService employeeService;
	private final PositionPermissionService positionPermissionService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String uri = request.getRequestURI();
		if (uri == null || !uri.startsWith("/admin") || "/admin/login".equals(uri)) {
			return true;
		}

		var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			return true;
		}

		Employee employee = employeeService.findByEmail(authentication.getName()).orElse(null);
		if (employee == null) {
			return true;
		}

		PositionPermission permission = positionPermissionService.findByPosition(employee.getPosition()).orElse(null);
		if (permission == null) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

		boolean allowed = switch (resolveSection(uri)) {
			case "dashboard" -> Boolean.TRUE.equals(permission.getCanDashboard());
			case "inquiries" -> Boolean.TRUE.equals(permission.getCanInquiries());
			case "members" -> Boolean.TRUE.equals(permission.getCanMembers());
			case "employees" -> Boolean.TRUE.equals(permission.getCanEmployees());
			case "shifts" -> Boolean.TRUE.equals(permission.getCanShifts());
			case "hamsters" -> Boolean.TRUE.equals(permission.getCanHamsters());
			case "products" -> Boolean.TRUE.equals(permission.getCanProducts());
			case "cafeCustomer" -> Boolean.TRUE.equals(permission.getCanCafeCustomer());
			case "cafeOrder" -> Boolean.TRUE.equals(permission.getCanCafe());
			default -> true;
		};

		if (!allowed) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
		return allowed;
	}

	private String resolveSection(String uri) {
		if ("/admin".equals(uri) || "/admin/".equals(uri) || uri.startsWith("/admin/dashboard")) return "dashboard";
		if (uri.startsWith("/admin/inquiries")) return "inquiries";
		if (uri.startsWith("/admin/members")) return "members";
		if (uri.startsWith("/admin/employees")) return "employees";
		if (uri.startsWith("/admin/shifts")) return "shifts";
		if (uri.startsWith("/admin/hamsters")) return "hamsters";
		if (uri.startsWith("/admin/products")) return "products";
		if (uri.startsWith("/admin/cafe/customer-screen")) return "cafeCustomer";
		if (uri.startsWith("/admin/cafe")) return "cafeOrder";
		return "";
	}
}
