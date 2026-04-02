package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.PositionPermission;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.PositionPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class AdminPermissionAdvice {

	private final EmployeeService employeeService;
	private final PositionPermissionService positionPermissionService;

	@ModelAttribute("canCreateEmployee")
	public boolean canCreateEmployee(Authentication authentication) {
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			return false;
		}
		return employeeService.isHeadOffice(authentication.getName());
	}

	@ModelAttribute("canCreateShift")
	public boolean canCreateShift(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanShifts);
	}

	@ModelAttribute("canCreateHamster")
	public boolean canCreateHamster(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanHamsters);
	}

	@ModelAttribute("canManageProductStock")
	public boolean canManageProductStock(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanProducts);
	}

	@ModelAttribute("canManageCafe")
	public boolean canManageCafe(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanCafe);
	}

	@ModelAttribute("canViewInquiries")
	public boolean canViewInquiries(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanInquiries);
	}

	@ModelAttribute("canViewMembers")
	public boolean canViewMembers(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanMembers);
	}

	@ModelAttribute("canViewEmployees")
	public boolean canViewEmployees(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanEmployees);
	}

	@ModelAttribute("canViewShifts")
	public boolean canViewShifts(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanShifts);
	}

	@ModelAttribute("canViewHamsters")
	public boolean canViewHamsters(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanHamsters);
	}

	@ModelAttribute("canViewProducts")
	public boolean canViewProducts(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanProducts);
	}

	@ModelAttribute("canViewCafe")
	public boolean canViewCafe(Authentication authentication) {
		return canAccess(authentication, PositionPermission::getCanCafe);
	}

	private boolean canAccess(
		Authentication authentication,
		java.util.function.Function<PositionPermission, Boolean> accessor
	) {
		try {
			if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
				return false;
			}
			String position = employeeService.findByEmail(authentication.getName())
				.map(employee -> employee.getPosition())
				.orElse(null);
			if (position == null || accessor == null) {
				return false;
			}
			return positionPermissionService.findByPosition(position)
				.map(accessor)
				.filter(Boolean::booleanValue)
				.orElse(false);
		} catch (RuntimeException ex) {
			return false;
		}
	}
}
