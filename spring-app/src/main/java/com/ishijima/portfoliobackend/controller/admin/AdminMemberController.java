package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.dto.MemberAnalyticsPoint;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

	private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

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

	@GetMapping("/analytics")
	public String analytics(Model model) {
		List<Member> members = memberService.findAll();
		List<MemberAnalyticsPoint> dailyPoints = buildDailyPoints(members);
		List<MemberAnalyticsPoint> monthlyPoints = buildMonthlyPoints(members);
		List<MemberAnalyticsPoint> yearlyPoints = buildYearlyPoints(members);

		model.addAttribute("dailyLabels", dailyPoints.stream().map(MemberAnalyticsPoint::getLabel).toList());
		model.addAttribute("dailyCounts", dailyPoints.stream().map(MemberAnalyticsPoint::getCount).toList());
		model.addAttribute("monthlyLabels", monthlyPoints.stream().map(MemberAnalyticsPoint::getLabel).toList());
		model.addAttribute("monthlyCounts", monthlyPoints.stream().map(MemberAnalyticsPoint::getCount).toList());
		model.addAttribute("yearlyLabels", yearlyPoints.stream().map(MemberAnalyticsPoint::getLabel).toList());
		model.addAttribute("yearlyCounts", yearlyPoints.stream().map(MemberAnalyticsPoint::getCount).toList());
		model.addAttribute("dailyPoints", dailyPoints);
		model.addAttribute("monthlyPoints", monthlyPoints);
		model.addAttribute("yearlyPoints", yearlyPoints);
		model.addAttribute("memberCount", members.size());
		model.addAttribute("activeCount", members.stream().filter(member -> "ACTIVE".equals(member.getStatus())).count());
		model.addAttribute("suspendedCount", members.stream().filter(member -> "SUSPENDED".equals(member.getStatus())).count());
		model.addAttribute("adminSection", "members");
		return "admin/member-analytics";
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

	private List<MemberAnalyticsPoint> buildDailyPoints(List<Member> members) {
		List<MemberAnalyticsPoint> points = new ArrayList<>();
		LocalDate today = LocalDate.now();
		for (int i = 13; i >= 0; i--) {
			LocalDate target = today.minusDays(i);
			long count = members.stream()
				.filter(member -> member.getCreatedAt() != null)
				.filter(member -> member.getCreatedAt().toLocalDate().equals(target))
				.count();
			points.add(new MemberAnalyticsPoint(target.format(DAY_FORMATTER), count));
		}
		return points;
	}

	private List<MemberAnalyticsPoint> buildMonthlyPoints(List<Member> members) {
		List<MemberAnalyticsPoint> points = new ArrayList<>();
		YearMonth current = YearMonth.now();
		for (int i = 11; i >= 0; i--) {
			YearMonth target = current.minusMonths(i);
			long count = members.stream()
				.filter(member -> member.getCreatedAt() != null)
				.filter(member -> YearMonth.from(member.getCreatedAt()).equals(target))
				.count();
			points.add(new MemberAnalyticsPoint(target.format(MONTH_FORMATTER), count));
		}
		return points;
	}

	private List<MemberAnalyticsPoint> buildYearlyPoints(List<Member> members) {
		List<MemberAnalyticsPoint> points = new ArrayList<>();
		int currentYear = LocalDate.now().getYear();
		for (int year = currentYear - 4; year <= currentYear; year++) {
			final int targetYear = year;
			long count = members.stream()
				.filter(member -> member.getCreatedAt() != null)
				.filter(member -> member.getCreatedAt().getYear() == targetYear)
				.count();
			points.add(new MemberAnalyticsPoint(String.valueOf(targetYear), count));
		}
		return points;
	}
}
