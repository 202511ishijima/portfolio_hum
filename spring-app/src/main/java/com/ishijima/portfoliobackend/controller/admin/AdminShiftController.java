package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Shift;
import com.ishijima.portfoliobackend.form.ShiftForm;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.ShiftService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/shifts")
@RequiredArgsConstructor
public class AdminShiftController {

	private final ShiftService shiftService;
	private final EmployeeService employeeService;

	@GetMapping
	public String list(Model model) {
		model.addAttribute("shifts", shiftService.findAll());
		model.addAttribute("employeeOptions", employeeService.findAll());
		model.addAttribute("adminSection", "shifts");
		return "admin/shifts";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		if (!model.containsAttribute("shiftForm")) {
			model.addAttribute("shiftForm", new ShiftForm(null, null, null, null, ""));
		}
		model.addAttribute("employeeOptions", employeeService.findAll());
		model.addAttribute("mode", "create");
		model.addAttribute("adminSection", "shifts");
		return "admin/shift-form";
	}

	@PostMapping("/new")
	public String create(
		@Valid @ModelAttribute("shiftForm") ShiftForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("employeeOptions", employeeService.findAll());
			model.addAttribute("mode", "create");
			model.addAttribute("adminSection", "shifts");
			return "admin/shift-form";
		}
		try {
			shiftService.create(form);
			redirectAttributes.addFlashAttribute("shiftMessage", "Shift created.");
			return "redirect:/admin/shifts";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("employeeOptions", employeeService.findAll());
			model.addAttribute("mode", "create");
			model.addAttribute("adminSection", "shifts");
			model.addAttribute("shiftError", ex.getMessage());
			return "admin/shift-form";
		}
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Shift shift = shiftService.findById(id);
		if (!model.containsAttribute("shiftForm")) {
			model.addAttribute("shiftForm", new ShiftForm(
				shift.getEmployeeId(),
				shift.getWorkDate(),
				shift.getStartTime(),
				shift.getEndTime(),
				shift.getNote()
			));
		}
		model.addAttribute("shift", shift);
		model.addAttribute("employeeOptions", employeeService.findAll());
		model.addAttribute("mode", "edit");
		model.addAttribute("adminSection", "shifts");
		return "admin/shift-form";
	}

	@PostMapping("/{id}/edit")
	public String update(
		@PathVariable Long id,
		@Valid @ModelAttribute("shiftForm") ShiftForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		Shift shift = shiftService.findById(id);
		if (bindingResult.hasErrors()) {
			model.addAttribute("shift", shift);
			model.addAttribute("employeeOptions", employeeService.findAll());
			model.addAttribute("mode", "edit");
			model.addAttribute("adminSection", "shifts");
			return "admin/shift-form";
		}
		try {
			shiftService.update(id, form);
			redirectAttributes.addFlashAttribute("shiftMessage", "Shift updated.");
			return "redirect:/admin/shifts";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("shift", shift);
			model.addAttribute("employeeOptions", employeeService.findAll());
			model.addAttribute("mode", "edit");
			model.addAttribute("adminSection", "shifts");
			model.addAttribute("shiftError", ex.getMessage());
			return "admin/shift-form";
		}
	}
}
