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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
			"message", "\u304A\u554F\u3044\u5408\u308F\u305B\u3092\u53D7\u3051\u4ED8\u3051\u307E\u3057\u305F\u3002"
		));
	}

	@GetMapping("/replies")
	public ResponseEntity<List<Map<String, Object>>> findRepliesByEmail(@RequestParam String email) {
		List<Map<String, Object>> responses = inquiryService.findRepliesByRecipientEmail(email).stream()
			.map(this::toReplyResponse)
			.toList();
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/thread")
	public ResponseEntity<List<InquiryThreadMessageResponse>> findThreadByEmail(@RequestParam String email) {
		String normalizedEmail = email == null ? "" : email.trim();
		if (normalizedEmail.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		List<InquiryThreadMessageResponse> thread = new ArrayList<>();
		inquiryService.findByEmail(normalizedEmail).forEach(inquiry -> thread.add(
			new InquiryThreadMessageResponse(
				"member",
				inquiry.getId(),
				null,
				inquiry.getSubject(),
				inquiry.getMessage(),
				inquiry.getCreatedAt()
			)
		));

		inquiryService.findRepliesByRecipientEmail(normalizedEmail).forEach(reply -> thread.add(
			new InquiryThreadMessageResponse(
				"admin",
				reply.getInquiryId(),
				reply.getId(),
				null,
				reply.getReply(),
				reply.getSentAt()
			)
		));

		thread.sort(Comparator
			.comparing(InquiryThreadMessageResponse::sentAt, Comparator.nullsLast(LocalDateTime::compareTo))
			.thenComparing(InquiryThreadMessageResponse::inquiryId, Comparator.nullsLast(Long::compareTo))
			.thenComparing(InquiryThreadMessageResponse::replyId, Comparator.nullsLast(Long::compareTo)));

		return ResponseEntity.ok(thread);
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

	public record InquiryThreadMessageResponse(
		String sender,
		Long inquiryId,
		Long replyId,
		String subject,
		String body,
		LocalDateTime sentAt
	) {
	}
}
