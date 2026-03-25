package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberPointAdjustForm;
import com.ishijima.portfoliobackend.form.MemberUpdateForm;
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

	@GetMapping("/{id}/edit")
	public String editMember(@PathVariable Long id, Model model) {
		Member member = memberService.findById(id);
		if (!model.containsAttribute("memberForm")) {
			model.addAttribute("memberForm", new MemberUpdateForm(
				member.getName(),
				member.getEmail(),
				""
			));
		}

		model.addAttribute("member", member);
		model.addAttribute("adminSection", "members");
		return "admin/member-edit";
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

	@PostMapping("/{id}/delete")
	public String deleteMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		memberService.deleteById(id);
		redirectAttributes.addFlashAttribute("memberMessage", "会員アカウントを削除しました。");
		return "redirect:/admin/members";
	}

	@PostMapping("/{id}/edit")
	public String updateMember(
		@PathVariable Long id,
		@Valid @ModelAttribute("memberForm") MemberUpdateForm form,
		BindingResult bindingResult,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		Member member = memberService.findById(id);

		if (bindingResult.hasErrors()) {
			model.addAttribute("member", member);
			model.addAttribute("adminSection", "members");
			return "admin/member-edit";
		}

		try {
			memberService.update(id, form);
			redirectAttributes.addFlashAttribute("memberMessage", "会員情報を更新しました。");
			return "redirect:/admin/members";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("member", member);
			model.addAttribute("adminSection", "members");
			model.addAttribute("memberError", ex.getMessage());
			return "admin/member-edit";
		}
	}
}
