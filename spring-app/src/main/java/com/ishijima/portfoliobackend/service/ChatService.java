package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.ChatMessage;

import java.util.List;

public interface ChatService {

	List<ChatMessage> findGeneralMessages();

	void sendGeneralMessage(Long senderEmployeeId, String body);
}
