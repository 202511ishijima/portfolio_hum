package com.ishijima.portfoliobackend.controller.api;

import com.ishijima.portfoliobackend.entity.Member;
import com.ishijima.portfoliobackend.form.MemberCreateForm;
import com.ishijima.portfoliobackend.form.MemberLoginForm;
import com.ishijima.portfoliobackend.form.MemberPointSyncForm;
import com.ishijima.portfoliobackend.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

	private final MemberService memberService;

	@PostMapping("/register")
	public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody MemberCreateForm form) {
		Member member = memberService.create(form);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
			"id", member.getId(),
			"name", member.getName(),
			"email", member.getEmail(),
			"points", member.getPoints(),
			"status", member.getStatus(),
			"message", "アカウントを作成しました。"
		));
	}

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody MemberLoginForm form) {
		Member member = memberService.authenticate(form.email(), form.password());
		return ResponseEntity.ok(Map.of(
			"id", member.getId(),
			"name", member.getName(),
			"email", member.getEmail(),
			"points", member.getPoints(),
			"status", member.getStatus(),
			"message", "ログインしました。"
		));
	}

	@PostMapping("/points")
	public ResponseEntity<Map<String, Object>> syncPoints(@Valid @RequestBody MemberPointSyncForm form) {
		Member member = memberService.adjustPointsByEmail(form.email(), form.delta());
		return ResponseEntity.ok(Map.of(
			"id", member.getId(),
			"email", member.getEmail(),
			"points", member.getPoints(),
			"status", member.getStatus(),
			"message", "ポイントを反映しました。"
		));
	}

	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> status(@RequestParam String email) {
		Member member = memberService.findByEmail(email);
		return ResponseEntity.ok(Map.of(
			"id", member.getId(),
			"name", member.getName(),
			"email", member.getEmail(),
			"points", member.getPoints(),
			"status", member.getStatus()
		));
	}
}
