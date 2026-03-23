package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberCreateForm;
import com.ishijima.portfoliobackend.form.MemberPointAdjustForm;
import com.ishijima.portfoliobackend.service.MemberService;
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

import java.util.List;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

	private final MemberService memberService;

	@GetMapping
	public String memberList(Model model) {
		if (!model.containsAttribute("memberForm")) {
			model.addAttribute("memberForm", new MemberCreateForm("", "", ""));
		}
		if (!model.containsAttribute("pointForm")) {
			model.addAttribute("pointForm", new MemberPointAdjustForm(0));
		}

		List<Member> members = memberService.findAll();
		long activeCount = members.stream().filter(member -> "ACTIVE".equals(member.getStatus())).count();
		long suspendedCount = members.stream().filter(member -> "SUSPENDED".equals(member.getStatus())).count();

		model.addAttribute("members", members);
		model.addAttribute("memberCount", members.size());
		model.addAttribute("activeCount", activeCount);
		model.addAttribute("suspendedCount", suspendedCount);
		model.addAttribute("adminSection", "members");
		return "admin/members";
	}

	@PostMapping
	public String createMember(
		@Valid @ModelAttribute("memberForm") MemberCreateForm form,
		BindingResult bindingResult,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.memberForm", bindingResult);
			redirectAttributes.addFlashAttribute("memberForm", form);
			redirectAttributes.addFlashAttribute("memberError", "入力内容を確認してください。");
			return "redirect:/admin/members";
		}

		try {
			memberService.create(form);
			redirectAttributes.addFlashAttribute("memberMessage", "会員アカウントを作成しました。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("memberForm", form);
			redirectAttributes.addFlashAttribute("memberError", ex.getMessage());
		}
		return "redirect:/admin/members";
	}

	@PostMapping("/{id}/status")
	public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		memberService.toggleStatus(id);
		redirectAttributes.addFlashAttribute("memberMessage", "会員ステータスを更新しました。");
		return "redirect:/admin/members";
	}

	@PostMapping("/{id}/points")
	public String adjustPoints(
		@PathVariable Long id,
		@Valid @ModelAttribute("pointForm") MemberPointAdjustForm form,
		BindingResult bindingResult,
		RedirectAttributes redirectAttributes
	) {
		if (bindingResult.hasErrors()) {
			redirectAttributes.addFlashAttribute("memberError", "ポイント数を確認してください。");
			return "redirect:/admin/members";
		}

		memberService.adjustPoints(id, form.delta());
		if (form.delta() >= 0) {
			redirectAttributes.addFlashAttribute("memberMessage", "ポイントを付与しました。");
		} else {
			redirectAttributes.addFlashAttribute("memberMessage", "ポイントを調整しました。");
		}
		return "redirect:/admin/members";
	}
}
