package com.ishijima.portfoliobackend.service;

import com.ishijima.portfoliobackend.entity.ChatMessage;
import com.ishijima.portfoliobackend.entity.ChatRoom;
import com.ishijima.portfoliobackend.mapper.ChatMessageMapper;
import com.ishijima.portfoliobackend.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

	private final ChatRoomMapper chatRoomMapper;
	private final ChatMessageMapper chatMessageMapper;
	private final EmployeeService employeeService;

	@Override
	public List<ChatMessage> findGeneralMessages() {
		Long roomId = getOrCreateGeneralRoomId();
		return chatMessageMapper.findByRoomId(roomId);
	}

	@Override
	@Transactional
	public void sendGeneralMessage(Long senderEmployeeId, String body) {
		if (body == null || body.isBlank()) {
			throw new IllegalArgumentException("Message is required.");
		}
		employeeService.findById(senderEmployeeId);
		Long roomId = getOrCreateGeneralRoomId();
		ChatMessage message = ChatMessage.builder()
			.roomId(roomId)
			.senderEmployeeId(senderEmployeeId)
			.body(body.trim())
			.sentAt(LocalDateTime.now())
			.build();
		chatMessageMapper.insert(message);
	}

	private Long getOrCreateGeneralRoomId() {
		return chatRoomMapper.findGeneralRoom()
			.map(ChatRoom::getId)
			.orElseGet(() -> {
				ChatRoom room = ChatRoom.builder()
					.name("General")
					.roomType("GENERAL")
					.createdAt(LocalDateTime.now())
					.build();
				chatRoomMapper.insert(room);
				return room.getId();
			});
	}
}
