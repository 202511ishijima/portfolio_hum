package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.form.InquiryStatusUpdateForm;
import com.ishijima.portfoliobackend.service.InquiryService;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminInquiryController {

	private final InquiryService inquiryService;

	@GetMapping("/login")
	public String loginPage() {
		return "admin/login";
	}

	@GetMapping("/inquiries")
	public String inquiryList(Model model) {
		model.addAttribute("inquiries", inquiryService.findAll());
		return "admin/inquiries";
	}

	@GetMapping("/inquiries/{id}")
	public String inquiryDetail(@PathVariable Long id, Model model) {
		Inquiry inquiry = inquiryService.findById(id);
		model.addAttribute("inquiry", inquiry);
		model.addAttribute("statusForm", new InquiryStatusUpdateForm(inquiry.getStatus()));
		return "admin/inquiry-detail";
	}

	@PostMapping("/inquiries/{id}/status")
	public String updateStatus(
		@PathVariable Long id,
		@Valid @ModelAttribute("statusForm") InquiryStatusUpdateForm form,
		BindingResult bindingResult,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("statusError", "対応状況の更新に失敗しました。");
			return "redirect:/admin/inquiries/" + id;
		}

		inquiryService.updateStatus(id, form.status());
		redirectAttributes.addFlashAttribute("statusMessage", "対応状況を更新しました。");
		return "redirect:/admin/inquiries/" + id;
	}
}
