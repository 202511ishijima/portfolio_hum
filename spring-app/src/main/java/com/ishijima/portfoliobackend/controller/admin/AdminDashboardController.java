package com.ishijima.portfoliobackend.controller.admin;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.entity.Hamster;
import com.ishijima.portfoliobackend.entity.HamsterStatus;
import com.ishijima.portfoliobackend.service.HamsterService;
import com.ishijima.portfoliobackend.service.InquiryService;
import com.ishijima.portfoliobackend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final InquiryService inquiryService;
	private final MemberService memberService;
	private final HamsterService hamsterService;

	@GetMapping({"", "/dashboard"})
	public String dashboard(Model model) {
		List<Inquiry> inquiries = inquiryService.findAll();
		List<Member> members = memberService.findAll();
		List<Hamster> hamsters = hamsterService.findAll(null, null, null);

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
		model.addAttribute("adminSection", "dashboard");
		return "admin/dashboard";
	}
}
