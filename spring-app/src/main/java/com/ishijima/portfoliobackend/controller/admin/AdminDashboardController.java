package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.form.ChatMessageForm;
import com.ishijima.portfoliobackend.service.ChatService;
import com.ishijima.portfoliobackend.service.EmployeeService;
import com.ishijima.portfoliobackend.service.HamsterService;
import com.ishijima.portfoliobackend.service.InquiryService;
import com.ishijima.portfoliobackend.service.MemberService;
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
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final InquiryService inquiryService;
	private final MemberService memberService;
	private final HamsterService hamsterService;
	private final EmployeeService employeeService;
	private final ChatService chatService;

	@GetMapping({"", "/dashboard"})
	public String dashboard(Model model, Principal principal) {
		List<Inquiry> inquiries = inquiryService.findAll();
		List<Member> members = memberService.findAll();
		List<Hamster> hamsters = hamsterService.findAll(null, null, null);
		String loginId = principal != null ? principal.getName() : null;
		var currentEmployee = loginId == null
			? null
			: employeeService.findByEmail(loginId).orElse(null);

		String adminDisplayName = currentEmployee == null
			? "ダッシュボード"
			: (currentEmployee.getName() != null && !currentEmployee.getName().isBlank()
				? currentEmployee.getName()
				: "ダッシュボード");

		long newInquiryCount = inquiries.stream().filter(inquiry -> "NEW".equals(inquiry.getStatus())).count();
		long inProgressInquiryCount = inquiries.stream().filter(inquiry -> "IN_PROGRESS".equals(inquiry.getStatus())).count();
		long activeMemberCount = members.stream().filter(member -> "ACTIVE".equals(member.getStatus())).count();
		long suspendedMemberCount = members.stream().filter(member -> "SUSPENDED".equals(member.getStatus())).count();
		long availableHamsterCount = hamsters.stream().filter(hamster -> hamster.getStatus() == HamsterStatus.AVAILABLE).count();

		model.addAttribute("inquiryCount", inquiries.size());
		model.addAttribute("newInquiryCount", newInquiryCount);
		model.addAttribute("inProgressInquiryCount", inProgressInquiryCount);
		model.addAttribute("memberCount", members.size());
		model.addAttribute("activeMemberCount", activeMemberCount);
		model.addAttribute("suspendedMemberCount", suspendedMemberCount);
		model.addAttribute("hamsterCount", hamsters.size());
		model.addAttribute("availableHamsterCount", availableHamsterCount);
		model.addAttribute("recentInquiries", inquiries.stream().limit(5).toList());
		model.addAttribute("recentMembers", members.stream().limit(5).toList());
		model.addAttribute("adminDisplayName", adminDisplayName);
		if (!model.containsAttribute("chatForm")) {
			model.addAttribute("chatForm", new ChatMessageForm(""));
		}
		model.addAttribute("currentEmployeeId", currentEmployee != null ? currentEmployee.getId() : null);
		model.addAttribute("messages", chatService.findGeneralMessages());
		model.addAttribute("adminSection", "dashboard");
		return "admin/dashboard";
	}

	@PostMapping("/dashboard/chat")
	public String sendDashboardChat(
		@Valid @ModelAttribute("chatForm") ChatMessageForm form,
		BindingResult bindingResult,
		Principal principal,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.chatForm", bindingResult);
			redirectAttributes.addFlashAttribute("chatForm", form);
			redirectAttributes.addFlashAttribute("chatOpen", true);
			return "redirect:/admin/dashboard";
		}

		try {
			Long senderId = employeeService.findByEmail(principal.getName())
				.orElseThrow(() -> new IllegalArgumentException("Employee not found."))
				.getId();
			chatService.sendGeneralMessage(senderId, form.body());
			redirectAttributes.addFlashAttribute("chatOpen", true);
			return "redirect:/admin/dashboard";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("chatError", ex.getMessage());
			redirectAttributes.addFlashAttribute("chatOpen", true);
			return "redirect:/admin/dashboard";
		}
	}
}
