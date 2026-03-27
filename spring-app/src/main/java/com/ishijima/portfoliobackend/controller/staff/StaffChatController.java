package com.ishijima.portfoliobackend.controller.staff;

import com.ishijima.portfoliobackend.entity.Employee;
import com.ishijima.portfoliobackend.form.ChatMessageForm;
import com.ishijima.portfoliobackend.service.ChatService;
import com.ishijima.portfoliobackend.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/staff/chat")
@RequiredArgsConstructor
public class StaffChatController {

	private final ChatService chatService;
	private final EmployeeService employeeService;

	@GetMapping
	public String chat(Model model, Principal principal) {
		Employee current = employeeService.findByEmail(principal.getName())
			.orElseThrow(() -> new IllegalArgumentException("Employee not found."));

		if (!model.containsAttribute("chatForm")) {
			model.addAttribute("chatForm", new ChatMessageForm(""));
		}
		model.addAttribute("currentEmployee", current);
		model.addAttribute("messages", chatService.findGeneralMessages());
		return "staff/chat";
	}

	@PostMapping
	public String send(
		@Valid @ModelAttribute("chatForm") ChatMessageForm form,
		BindingResult bindingResult,
		Principal principal,
		RedirectAttributes redirectAttributes
	) {
		Employee current = employeeService.findByEmail(principal.getName())
			.orElseThrow(() -> new IllegalArgumentException("Employee not found."));

		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.chatForm", bindingResult);
			redirectAttributes.addFlashAttribute("chatForm", form);
			return "redirect:/staff/chat";
		}

		try {
			chatService.sendGeneralMessage(current.getId(), form.body());
			return "redirect:/staff/chat";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("chatError", ex.getMessage());
			return "redirect:/staff/chat";
		}
	}
}
