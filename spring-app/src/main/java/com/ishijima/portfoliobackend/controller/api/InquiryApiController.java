package com.ishijima.portfoliobackend.controller.api;

import com.ishijima.portfoliobackend.entity.Inquiry;
import com.ishijima.portfoliobackend.form.InquiryForm;
import com.ishijima.portfoliobackend.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
