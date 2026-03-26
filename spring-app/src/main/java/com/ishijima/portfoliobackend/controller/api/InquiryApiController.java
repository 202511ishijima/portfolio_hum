package com.ishijima.portfoliobackend.controller.api;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.entity.InquiryReply;
import com.ishijima.portfoliobackend.form.InquiryForm;
import com.ishijima.portfoliobackend.service.InquiryService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryApiController {

	private final InquiryService inquiryService;

	@PostMapping
	public ResponseEntity<Map<String, Object>> createInquiry(@Valid @RequestBody InquiryForm form) {
		Inquiry inquiry = inquiryService.create(form);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
			"id", inquiry.getId(),
			"message", "お問い合わせを受け付けました。"
		));
	}

	@GetMapping("/replies")
	public ResponseEntity<List<Map<String, Object>>> findRepliesByEmail(@RequestParam String email) {
		List<Map<String, Object>> responses = inquiryService.findRepliesByRecipientEmail(email).stream()
			.map(this::toReplyResponse)
			.toList();
		return ResponseEntity.ok(responses);
	}

	private Map<String, Object> toReplyResponse(InquiryReply reply) {
		return Map.of(
			"id", reply.getId(),
			"inquiryId", reply.getInquiryId(),
			"recipientEmail", reply.getRecipientEmail(),
			"reply", reply.getReply(),
			"sentAt", reply.getSentAt()
		);
	}
}
