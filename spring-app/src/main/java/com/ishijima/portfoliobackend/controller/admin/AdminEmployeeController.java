package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.entity.PositionPermission;
import com.ishijima.portfoliobackend.form.EmployeeCreateForm;
import com.ishijima.portfoliobackend.form.EmployeeUpdateForm;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.PositionPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

	private final EmployeeService employeeService;
	private final PositionPermissionService positionPermissionService;

	@GetMapping
	public String list(Model model, Principal principal) {
		List<Employee> employees = employeeService.findAll();
		long activeCount = employees.stream().filter(e -> Boolean.TRUE.equals(e.getActive())).count();
		long inactiveCount = employees.size() - activeCount;

		model.addAttribute("employees", employees);
		model.addAttribute("employeeCount", employees.size());
		model.addAttribute("activeCount", activeCount);
		model.addAttribute("inactiveCount", inactiveCount);
		model.addAttribute("canCreateEmployee", employeeService.isHeadOffice(principal.getName()));
		model.addAttribute("adminSection", "employees");
		return "admin/employees";
	}

	@GetMapping("/permissions")
	public String permissions(Model model, Principal principal, RedirectAttributes redirectAttributes) {
		if (!employeeService.isHeadOffice(principal.getName())) {
			redirectAttributes.addFlashAttribute("employeeError", "Only head office can open position permissions.");
			return "redirect:/admin/employees";
		}
		model.addAttribute("permissions", positionPermissionService.findAll());
		model.addAttribute("adminSection", "employees");
		return "admin/employee-permissions";
	}

	@PostMapping("/permissions/{position}")
	public String updatePermission(
		@PathVariable String position,
		@RequestParam(defaultValue = "false") boolean canDashboard,
		@RequestParam(defaultValue = "false") boolean canInquiries,
		@RequestParam(defaultValue = "false") boolean canMembers,
		@RequestParam(defaultValue = "false") boolean canEmployees,
		@RequestParam(defaultValue = "false") boolean canShifts,
		@RequestParam(defaultValue = "false") boolean canHamsters,
		@RequestParam(defaultValue = "false") boolean canProducts,
		@RequestParam(defaultValue = "false") boolean canCafe,
		Principal principal,
		RedirectAttributes redirectAttributes
	) {
		if (!employeeService.isHeadOffice(principal.getName())) {
			redirectAttributes.addFlashAttribute("employeeError", "Only head office can update position permissions.");
			return "redirect:/admin/employees";
		}
		try {
			PositionPermission permission = PositionPermission.builder()
				.position(position)
				.canDashboard(canDashboard)
				.canInquiries(canInquiries)
				.canMembers(canMembers)
				.canEmployees(canEmployees)
				.canShifts(canShifts)
				.canHamsters(canHamsters)
				.canProducts(canProducts)
				.canCafe(canCafe)
				.build();
			positionPermissionService.update(permission, principal.getName());
			redirectAttributes.addFlashAttribute("employeeMessage", "Position permission updated.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
		}
		return "redirect:/admin/employees/permissions";
	}

	@GetMapping("/new")
	public String newForm(Model model, Principal principal, RedirectAttributes redirectAttributes) {
		if (!employeeService.isHeadOffice(principal.getName())) {
			redirectAttributes.addFlashAttribute("employeeError", "Only head office can create employees.");
			return "redirect:/admin/employees";
		}

		if (!model.containsAttribute("employeeForm")) {
			model.addAttribute("employeeForm", new EmployeeCreateForm("", "", "", "一般従業員", true));
		}
		model.addAttribute("positionOptions", employeeService.getAssignablePositions(principal.getName()));
		model.addAttribute("mode", "create");
		model.addAttribute("adminSection", "employees");
		return "admin/employee-form";
	}

	@PostMapping("/new")
	public String create(
		@Valid @ModelAttribute("employeeForm") EmployeeCreateForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes,
		Principal principal
	) {
		if (!employeeService.isHeadOffice(principal.getName())) {
			redirectAttributes.addFlashAttribute("employeeError", "Only head office can create employees.");
			return "redirect:/admin/employees";
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("positionOptions", employeeService.getAssignablePositions(principal.getName()));
			model.addAttribute("mode", "create");
			model.addAttribute("adminSection", "employees");
			return "admin/employee-form";
		}
		try {
			employeeService.create(form, principal.getName());
			redirectAttributes.addFlashAttribute("employeeMessage", "Employee account created.");
			return "redirect:/admin/employees";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("positionOptions", employeeService.getAssignablePositions(principal.getName()));
			model.addAttribute("mode", "create");
			model.addAttribute("adminSection", "employees");
			model.addAttribute("employeeError", ex.getMessage());
			return "admin/employee-form";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, Principal principal) {
		Employee employee = employeeService.findById(id);
		if (!employeeService.canManagePosition(principal.getName(), employee.getPosition())) {
			redirectAttributes.addFlashAttribute("employeeError", "You can edit only employees below your position.");
			return "redirect:/admin/employees";
		}

		if (!model.containsAttribute("employeeForm")) {
			model.addAttribute("employeeForm", new EmployeeUpdateForm(
				employee.getName(),
				employee.getEmail(),
				"",
				employee.getPosition(),
				Boolean.TRUE.equals(employee.getActive())
			));
		}
		model.addAttribute("positionOptions", employeeService.getAssignablePositions(principal.getName()));
		model.addAttribute("employee", employee);
		model.addAttribute("mode", "edit");
		model.addAttribute("adminSection", "employees");
		return "admin/employee-form";
	}

	@PostMapping("/{id}/edit")
	public String update(
		@PathVariable Long id,
		@Valid @ModelAttribute("employeeForm") EmployeeUpdateForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes,
		Principal principal
	) {
		Employee employee = employeeService.findById(id);
		if (!employeeService.canManagePosition(principal.getName(), employee.getPosition())) {
			redirectAttributes.addFlashAttribute("employeeError", "You can edit only employees below your position.");
			return "redirect:/admin/employees";
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute("positionOptions", employeeService.getAssignablePositions(principal.getName()));
			model.addAttribute("employee", employee);
			model.addAttribute("mode", "edit");
			model.addAttribute("adminSection", "employees");
			return "admin/employee-form";
		}
		try {
			employeeService.update(id, form, principal.getName());
			redirectAttributes.addFlashAttribute("employeeMessage", "Employee account updated.");
			return "redirect:/admin/employees";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("positionOptions", employeeService.getAssignablePositions(principal.getName()));
			model.addAttribute("employee", employee);
			model.addAttribute("mode", "edit");
			model.addAttribute("adminSection", "employees");
			model.addAttribute("employeeError", ex.getMessage());
			return "admin/employee-form";
		}
	}

	@PostMapping("/{id}/active")
	public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes, Principal principal) {
		try {
			employeeService.toggleActive(id, principal.getName());
			redirectAttributes.addFlashAttribute("employeeMessage", "Employee active status updated.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
		}
		return "redirect:/admin/employees";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes, Principal principal) {
		try {
			employeeService.delete(id, principal.getName());
			redirectAttributes.addFlashAttribute("employeeMessage", "Employee account deleted.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
		}
		return "redirect:/admin/employees";
	}

}
