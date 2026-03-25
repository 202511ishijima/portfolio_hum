package com.ishijima.portfoliobackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryReply {
	private Long id;
	private Long inquiryId;
	private String recipientEmail;
	private String reply;
	private LocalDateTime sentAt;
	private LocalDateTime createdAt;
}
