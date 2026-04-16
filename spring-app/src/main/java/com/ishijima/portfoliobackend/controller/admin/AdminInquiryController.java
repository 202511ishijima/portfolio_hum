package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.entity.InquiryReply;
import com.ishijima.portfoliobackend.form.InquiryStatusUpdateForm;
import com.ishijima.portfoliobackend.form.InquiryReplyForm;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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
	public String inquiryList(
		@RequestParam(defaultValue = "createdAt") String sort,
		@RequestParam(defaultValue = "desc") String order,
		@RequestParam(defaultValue = "false") boolean showCompleted,
		Model model
	) {
		List<Inquiry> allInquiries = inquiryService.findAll();
		List<Inquiry> visibleInquiries = showCompleted
			? allInquiries
			: allInquiries.stream()
				.filter(inquiry -> !"COMPLETED".equals(inquiry.getStatus()))
				.toList();
		List<Inquiry> sortedInquiries = sortInquiries(visibleInquiries, sort, order);
		long newCount = allInquiries.stream().filter(inquiry -> "NEW".equals(inquiry.getStatus())).count();
		long inProgressCount = allInquiries.stream().filter(inquiry -> "IN_PROGRESS".equals(inquiry.getStatus())).count();
		long completedCount = allInquiries.stream().filter(inquiry -> "COMPLETED".equals(inquiry.getStatus())).count();

		model.addAttribute("inquiries", sortedInquiries);
		model.addAttribute("totalInquiryCount", allInquiries.size());
		model.addAttribute("newInquiryCount", newCount);
		model.addAttribute("inProgressInquiryCount", inProgressCount);
		model.addAttribute("completedInquiryCount", completedCount);
		model.addAttribute("sort", normalizeSort(sort));
		model.addAttribute("order", normalizeOrder(order));
		model.addAttribute("showCompleted", showCompleted);
		model.addAttribute("adminSection", "inquiries");
		return "admin/inquiries";
	}

	@GetMapping("/inquiries/{id}")
	public String inquiryDetail(@PathVariable Long id, Model model) {
		Inquiry inquiry = inquiryService.findById(id);
		List<InquiryReply> replyHistory = inquiryService.findRepliesByInquiryId(id);
		model.addAttribute("inquiry", inquiry);
		model.addAttribute("replyHistory", replyHistory);
		model.addAttribute("statusForm", new InquiryStatusUpdateForm(inquiry.getStatus()));
		model.addAttribute("replyForm", new InquiryReplyForm(""));
		model.addAttribute("adminSection", "inquiries");
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

	@PostMapping("/inquiries/{id}/reply")
	public String sendReply(
		@PathVariable Long id,
		@Valid @ModelAttribute("replyForm") InquiryReplyForm form,
		BindingResult bindingResult,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("replyError", "返信内容を確認してください。");
			return "redirect:/admin/inquiries/" + id;
		}

		inquiryService.sendReply(id, form.reply());
		redirectAttributes.addFlashAttribute("replyMessage", "返信を送信し、履歴に追加しました。");
		return "redirect:/admin/inquiries/" + id;
	}

	private List<Inquiry> sortInquiries(List<Inquiry> inquiries, String sort, String order) {
		Comparator<Inquiry> comparator = switch (normalizeSort(sort)) {
			case "id" -> Comparator.comparing(Inquiry::getId, Comparator.nullsLast(Long::compareTo));
			case "name" -> Comparator.comparing(Inquiry::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "email" -> Comparator.comparing(Inquiry::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "subject" -> Comparator.comparing(Inquiry::getSubject, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "status" -> Comparator.comparing(Inquiry::getStatus, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			default -> Comparator.comparing(Inquiry::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
		};

		if ("desc".equals(normalizeOrder(order))) {
			comparator = comparator.reversed();
		}

		return inquiries.stream().sorted(comparator).toList();
	}

	private String normalizeSort(String sort) {
		return switch (sort) {
			case "id", "name", "email", "subject", "status", "createdAt" -> sort;
			default -> "createdAt";
		};
	}

	private String normalizeOrder(String order) {
		return "asc".equalsIgnoreCase(order) ? "asc" : "desc";
	}
}
