package com.ishijima.portfoliobackend.controller.admin;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class AdminPermissionAdvice {

	@ModelAttribute("canCreateEmployee")
	public boolean canCreateEmployee(Authentication authentication) {
		return hasRole(authentication, "ROLE_ADMIN");
	}

	@ModelAttribute("canCreateShift")
	public boolean canCreateShift(Authentication authentication) {
		return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_STAFF_MANAGER");
	}

	@ModelAttribute("canCreateHamster")
	public boolean canCreateHamster(Authentication authentication) {
		return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_STAFF_MANAGER");
	}

	@ModelAttribute("canManageProductStock")
	public boolean canManageProductStock(Authentication authentication) {
		return hasRole(authentication, "ROLE_ADMIN") || hasRole(authentication, "ROLE_STAFF_MANAGER");
	}

	private boolean hasRole(Authentication authentication, String authority) {
		if (authentication == null || authentication.getAuthorities() == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
			.anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
	}
}
