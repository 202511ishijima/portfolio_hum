package com.ishijima.portfoliobackend.controller.staff;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/staff/shifts")
@RequiredArgsConstructor
public class StaffShiftController {

	private final ShiftService shiftService;
	private final EmployeeService employeeService;

	@GetMapping("/mine")
	public String mine(Model model, Principal principal) {
		Employee current = employeeService.findByEmail(principal.getName())
			.orElseThrow(() -> new IllegalArgumentException("Employee not found."));

		model.addAttribute("employee", current);
		model.addAttribute("shifts", shiftService.findByEmployeeId(current.getId()));
		return "staff/my-shifts";
	}
}
