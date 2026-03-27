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
public class ChatMessage {
	private Long id;
	private Long roomId;
	private Long senderEmployeeId;
	private String senderName;
	private String body;
	private LocalDateTime sentAt;
}
